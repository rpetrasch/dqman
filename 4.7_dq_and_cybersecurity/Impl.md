# Implementation: Email Session Security — DQ & Cybersecurity Example

## Overview

This module demonstrates how **data quality principles** directly apply to cybersecurity.
The scenario is a corporate email user (`john.doe`) whose sessions are monitored across
four log sources.  The pipeline detects account takeover, impossible travel, session
hijacking, data exfiltration, brute-force attacks and account sharing.

The DQ angle: correctness, completeness, consistency and lineage traceability are not
just data warehouse concerns — they are the foundation of any credible security detection
system.

---

## Architecture

```
[Email Logs]  [IdP Logs]  [Network Logs]  [Device GeoIP]
      │              │              │               │
      └──────────────┴──────────────┴───────────────┘
                            │
                     [Kafka Topics]       ← logs.email / logs.idp
                     (raw log events)       logs.network / logs.geolocation
                            │
               [Spring Boot Pipeline Service]
                            │
           ┌────────────────┼────────────────┐
           │                │                │
      [Parsing &      [Entity            [Enrichment]
      Normalization]  Resolution]        resolved location,
      UTC, event_type User/Device via    risk score delta
      session_offset  Caffeine cache
           │                │                │
           └────────────────┴────────────────┘
                            │
                  [PostgreSQL + TimescaleDB]
                            │
           ┌────────────────┼──────────────────┐
           │                │                  │
      [Master Data]   [raw_events]   [normalized_events]
      users, devices  immutable,      hypertable, 3 timestamps,
      locations, apps JSONB payload   raw_event_id lineage FK
                            │
                   [Detection Engine]
                            │
             ┌──────────────┴──────────────┐
             │                             │
     [In-Session Rules]          [Post-Session CTEs]
     per-event streaming         @Scheduled on closed sessions
             │                             │
             └──────────────┬──────────────┘
                            │
                  [Kafka: security.alerts]
                            │
               ┌────────────┴────────────┐
          [Kafbat UI]              [alerts table]
          real-time dashboard      PostgreSQL, queryable
```

---

## Implemented Artefacts

### Infrastructure (`docker/`)

| File | Purpose |
|---|---|
| `docker-compose.yml` | Orchestrates all four containers with health-check ordering |
| `init.sql` | Creates the full schema, TimescaleDB hypertables, indexes, seed master data and a continuous aggregate for baseline computation |
| `Dockerfile` | Eclipse Temurin JRE 24 image for the Spring Boot fat JAR |

**Containers:**

| Container | Image | Port |
|---|---|---|
| `security-timescaledb` | `timescale/timescaledb:latest-pg16` | 5432 |
| `security-kafka` | `apache/kafka:latest` (KRaft, no ZooKeeper) | 9092 |
| `security-kafbat-ui` | `ghcr.io/kafbat/kafka-ui:latest` | 8080 |
| `security-pipeline` | Built from `docker/Dockerfile` | 8081 |

### Configuration (`src/main/resources/application.yml`)

All operational thresholds and topic names are externalized:

```
app.kafka.topics.*                  — four input topics + alert topic
app.detection.failed-login-*        — brute-force window and count
app.detection.excessive-download-*  — exfiltration threshold
app.detection.impossible-travel-*   — travel time threshold (hours)
app.detection.smtp-allowlist        — known-good SMTP relay list
app.scheduling.post-session-delay-ms — post-session job interval
app.generator.enabled / interval-ms — test data generation toggle
```

---

### Data Model (`docker/init.sql`)

#### Master Data (static, loaded at startup)

| Table | Columns | Notes |
|---|---|---|
| `users` | id, username, email, home_country_code, normal_work_start/end, home_timezone | Anchor for entity resolution and off-hours detection |
| `devices` | id, device_id, hostname, trust_level, owner_id | CORPORATE / PERSONAL / UNKNOWN; drives new-device rule |
| `known_locations` | id, name, location_type, country_code, ip_cidr, lat, lon | IP prefix matching; OFFICE / HOME / VPN |
| `applications` | id, name, app_type, version | EMAIL_CLIENT / IDP / NETWORK / GEOLOCATION |

#### Session-Centric Model

| Table | Key columns | Notes |
|---|---|---|
| `sessions` | session_id (PK), user_id, start/end_time_utc, duration_sec, status, risk_score, processed, context JSONB | `context` stores login-time IP, device, country — immutable snapshot used by CTE queries |

#### Event Store (TimescaleDB hypertables)

| Table | Partition column | Purpose |
|---|---|---|
| `raw_events` | `ingestion_time` | Immutable; full original log payload as JSONB; `source_file` + `source_offset` for log-line traceability |
| `normalized_events` | `event_time_utc` | Enriched; `raw_event_id` FK closes the lineage chain; three explicit timestamps (event_time, ingestion_time, processing_time) |

**Three-timestamp model** (a core DQ pattern for event stores):

| Timestamp | Meaning |
|---|---|
| `event_time_utc` | When the event happened — source system clock, normalized to UTC |
| `ingestion_time` | When Kafka received the record — set by the producer |
| `processing_time` | When the pipeline wrote it to the database |

The gap between timestamps exposes clock skew, ingestion lag and processing latency — all relevant for forensic timelines.

#### Supporting Tables

| Table | Purpose |
|---|---|
| `entity_edges` | Relationship graph (USER owns DEVICE, SESSION logged in from LOCATION) — recursive CTE target |
| `alerts` | Persisted alert records with full evidence JSONB |

---

### Java Source Files

#### Entry Point

**`Main.java`** — `@SpringBootApplication` + `@EnableCaching` + `@EnableScheduling`.

#### Model Layer (`model/`)

| Class | Type | Description |
|---|---|---|
| `User` | JPA entity | Canonical user from master data; home country and work-hours fields drive detection rules |
| `Device` | JPA entity | MDM-registered device with trust level and owner FK |
| `KnownLocation` | JPA entity | Named IP range with country code; used for location resolution and travel detection |
| `EmailApplication` | JPA entity | Source system registration (email client, IdP, network, MDM) |
| `Session` | JPA entity | First-class correlation unit; `context` JSONB holds login-time snapshot |
| `RawLogEvent` | POJO | Deserialized Kafka message; carries all three timestamps and the original payload map |
| `Alert` | POJO (Builder) | Alert event published to Kafka and written to DB; `evidence` JSONB closes the drill-down chain |

#### Repository Layer (`repository/`)

| Interface | Spring Data method |
|---|---|
| `UserRepository` | `findByUsername(String)` |
| `DeviceRepository` | `findByDeviceId(String)`, `findByOwner_Id(Long)` |
| `KnownLocationRepository` | `findByIpPrefix(String)` (JPQL prefix match) |
| `SessionRepository` | `findByStatusAndProcessedFalse(String)`, `findByUserIdAndStatus(Long, String)` |

All repository results are served from a Caffeine cache in `EntityResolutionService` so that high-throughput Kafka consumption does not incur a DB round-trip per event.

#### Consumer Layer (`consumer/`)

**`EventConsumer`** — one class, four `@KafkaListener` methods, single consumer group `pipeline-service`.

```java
@KafkaListener(topics = "${app.kafka.topics.email}")    consumeEmailLog()
@KafkaListener(topics = "${app.kafka.topics.idp}")      consumeIdpLog()
@KafkaListener(topics = "${app.kafka.topics.network}")  consumeNetworkLog()
@KafkaListener(topics = "${app.kafka.topics.geolocation}") consumeGeolocationLog()
```

Each method deserializes the JSON string into a `RawLogEvent` POJO, stamps `ingestionTime` from the Kafka record timestamp (preserving the original ingest time, not the consumer poll time), and delegates to `EventPipelineService`.

#### Pipeline Layer (`pipeline/`)

**`EntityResolutionService`** — resolves raw username/deviceId/IP strings to master data entities via `@Cacheable` Caffeine lookups.  Cache TTL is 300 seconds (configurable).

**`EventPipelineService`** — `@Transactional` per-event orchestrator:

1. **Persist raw event** via `JdbcTemplate` → `raw_events` hypertable (immutable, idempotent with `ON CONFLICT DO NOTHING`)
2. **Entity resolution** — user, location from cache
3. **Derive event type** — from payload field, fall back to source system
4. **Risk scoring** — additive per-event delta accumulated on the session
5. **Session upsert** — find or create via `SessionRepository`
6. **Persist normalized event** — `JdbcTemplate` → `normalized_events` with `raw_event_id` FK and enriched JSONB payload
7. **In-session detection** — delegate to `InSessionDetectionService`

JPA is used for master data (users, devices, locations) and session management; `JdbcTemplate` is used for the event hypertable writes and CTE-based detection queries where JPA would be insufficient.

#### Detection Layer (`detection/`)

**`InSessionDetectionService`** — five streaming rules, evaluated per event:

| Rule | Trigger | Severity |
|---|---|---|
| `NEW_DEVICE` | `device_id` not in user's MDM device list | MEDIUM |
| `IP_CHANGE_MID_SESSION` | Event IP ≠ session login IP | HIGH |
| `NEW_COUNTRY` | GeoIP country ≠ user's home country | HIGH |
| `FAILED_LOGIN_SPIKE` | ≥ N failures within the configured window | CRITICAL |
| `SUSPICIOUS_SMTP_RELAY` | SMTP destination not on allowlist | HIGH |

**`PostSessionDetectionService`** — `@Scheduled` (fixed-delay, default 60 s) aggregate rules on closed sessions using `JdbcTemplate` + recursive CTEs:

| Rule | Logic | Severity |
|---|---|---|
| `IMPOSSIBLE_TRAVEL` | Two sessions, same user, different countries, gap < threshold — recursive self-join on `sessions` | CRITICAL |
| `EXCESSIVE_DOWNLOADS` | `COUNT(ATTACHMENT_DOWNLOAD)` > threshold within session | HIGH |
| `SESSION_DURATION_OUTLIER` | `duration_sec` > μ + N·σ of user's historical sessions | MEDIUM |
| `OFF_HOURS_ACTIVITY` | Events outside `normal_work_start`/`normal_work_end` in user's timezone | MEDIUM |
| `ACCOUNT_SHARING` | Same `user_id`, overlapping session windows, different `device_id` | HIGH |

After all rules are evaluated the session `processed` flag is set to `true` to prevent duplicate alerts on the next tick.

#### Alert Layer (`alert/`)

**`AlertPublisher`** — publishes each `Alert` to the `security.alerts` Kafka topic (consumed by Kafbat UI and downstream SIEM) and writes it to the `alerts` PostgreSQL table.  The `evidence` JSONB field in every alert contains:

- `eventIds` — list of `normalized_events.event_id` records that triggered the alert
- `rawEventIds` — corresponding `raw_events.event_id` records (immutable originals)
- `details` — human-readable description of the violation

This closes the full forensic drill-down chain:
**Alert → Session → Normalized Events → Raw Events → Original Log Line**

#### Test Data Generator (`generator/`)

**`TestDataGenerator`** — active only when `app.generator.enabled=true`; cycles through seven scenarios on each scheduled tick:

| Scenario | What it produces |
|---|---|
| Normal session | Login, 5–15 email reads, 0–3 downloads, logout from Berlin corporate IP |
| Impossible travel | Session from Berlin (10.0.0.x) + session 90 min later from Bangkok (203.150.x.x) |
| Session hijacking | Known device at login → unknown device + external IP mid-session |
| Data exfiltration | 60 `ATTACHMENT_DOWNLOAD` events + SMTP relay to `mail.attacker-domain.ru` |
| Brute force | 12 `FAILED` IdP auth events followed by one `SUCCESS` |
| Off-hours access | Login at 03:00 UTC, 15 attachment downloads |
| Account sharing | Two concurrent sessions, same user, different devices |

---

## How to Run

```bash
# 1. Build the Spring Boot fat JAR
cd 4.7_dq_and_cybersecurity
mvn package -DskipTests

# 2. Start all containers
cd docker
docker compose up -d

# 3. Monitor
#    Kafbat UI (Kafka topic browser + alert dashboard): http://localhost:8080
#    Pipeline health endpoint:                         http://localhost:8081/actuator/health
#    PostgreSQL:                                        psql -h localhost -U security_user -d security_db
```

Kafka topics are auto-created by the broker.  The test data generator starts automatically
and cycles through all attack scenarios every 8 seconds (configurable via `app.generator.interval-ms`).

---

## Key DQ Principles Demonstrated

| DQ Principle | How it appears here |
|---|---|
| **Lineage traceability** | `raw_event_id` FK from normalized to raw event; `evidence.rawEventIds` in alerts |
| **Three-timestamp model** | `event_time_utc`, `ingestion_time`, `processing_time` on every event — exposes clock skew and processing lag |
| **Immutability** | `raw_events` is write-once; `ON CONFLICT DO NOTHING` enforces it |
| **Entity resolution** | Raw usernames / IPs resolved to canonical master data before any analysis |
| **Master data separation** | Users, devices, locations loaded once; not duplicated per event |
| **JSONB for heterogeneous payloads** | Event attributes vary by type; JSONB avoids EAV anti-pattern while preserving query flexibility |
| **Idempotent writes** | `ON CONFLICT DO NOTHING` on both event tables; safe for at-least-once Kafka delivery |
| **Baseline and outlier detection** | TimescaleDB continuous aggregate (`user_session_baselines`) computes rolling μ/σ for session duration and download counts |
