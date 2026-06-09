
# Specification: Email Session Security — A Practical Data Quality & Cybersecurity Example

## 1. Use Case Description

A corporate user (`john.doe`) operates an email client (e.g., Outlook/Exchange). We want to detect suspicious activity in real time and post-session — covering account takeover, impossible travel, session hijacking, data exfiltration via attachments, and SMTP relay abuse.

**Four raw data sources are merged per session:**

| Source | Key Signals |
|---|---|
| Email client logs | Login time, IP, device, email events |
| Identity Provider (IdP) logs | Auth success/failure, MFA status, expected location |
| Network logs | External SMTP connections, suspicious destinations |
| Device geolocation | GPS/network-derived location per event |

The core challenge is not just ingestion — it's building a semantic model that correlates these sources across session boundaries while preserving full forensic traceability back to the original raw log line.

## 2. Pipeline Description

The pipeline has two planes: a **streaming plane** (Kafka) and a **storage/analysis plane** (PostgreSQL + TimescaleDB).

```
[Email Logs]  [IdP Logs]  [Network Logs]  [Device GeoIP]
      │              │              │               │
      └──────────────┴──────────────┴───────────────┘
                            │
                     [Kafka Topics]
                     (raw log events)
                            │
                   [Java Pipeline Service]
                            │
           ┌────────────────┼────────────────┐
           │                │                │
      [Parsing &      [Entity            [Enrichment]
      Normalization]  Resolution]        GeoIP, threat intel,
      UTC, IPv4/v6,   User/Device/        risk scoring
      Location, IDs   App MDM lookup
           │                │                │
           └────────────────┴────────────────┘
                            │
                  [PostgreSQL + TimescaleDB]
                            │
           ┌────────────────┼──────────────┐
           │                │              │
      [Master Data]   [Raw Events]   [Normalized Events]
      users, devices,  (immutable)    (Timescale hypertable)
      locations, apps               sessions, events, JSONB payload
                            │
                   [Detection Engine]
                   (SQL + Recursive CTEs)
                            │
                   [Kafka Alert Topic]
                            │
               ┌────────────┴────────────┐
          [Kafbat UI]              [Downstream]
          (dashboard)              (SIEM, ticketing)
```

**Two detection modes:**
- **During session:** Streaming detection as events arrive (real-time rules via Java consumer)
- **Post-session:** Batch/CTE-based analysis on the closed session aggregate


## 3. Data Model

**Master Data (static, initial DB import):**
- `users` — canonical user entity (resolved from email, SID, username aliases)
- `devices` — known corporate devices with trust level
- `applications` — email clients, IdP systems, network sources
- `known_locations` — corporate offices, home office ranges, VPN endpoints

**Session-centric model (Session as first-class entity):**
- `sessions` — `session_id`, `user_id`, `start_time_utc`, `end_time_utc`, `duration_sec`, `status`, `risk_score`
- `session_context` — JSONB bag for per-session metadata (initial IP, device, location at login)

**Event store (TimescaleDB hypertable, partitioned by `event_time`):**
- `raw_events` — immutable; full original log payload as JSONB, `source_system`, `source_file`, `source_offset`, `ingestion_time`
- `normalized_events` — `event_id`, `raw_event_id` (FK → lineage), `session_id`, `user_id`, `event_time_utc`, `ingestion_time`, `processing_time`, `event_type`, `device_id`, `ip`, `location_id`, `session_offset_sec`, `payload` JSONB

**Three timestamps on every event:**
- `event_time_utc` — when it happened (source clock, normalized to UTC)
- `ingestion_time` — when Kafka received it
- `processing_time` — when the pipeline wrote it to the DB

**Relationship / graph edges (recursive CTE target):**
- `entity_edges` — `source_type`, `source_id`, `target_type`, `target_id`, `relationship_type`, `valid_from`, `valid_to`

JSONB is used for the raw payload (preserving original evidence) and for flexible event attributes that vary by event type (email read vs. attachment download vs. SMTP connection have different fields).


## 4. Analysis and Detection Logic

**In-session rules (applied per event as it streams in):**

| Rule | Logic |
|---|---|
| New device | Current event `device_id` ∉ user's known devices |
| IP change mid-session | Event IP ≠ session login IP |
| New country | GeoIP country ≠ user's home country |
| Failed login spike | ≥3 failed auth events within 5-minute window |
| Suspicious SMTP destination | Network event to non-allowlisted external relay |

**Post-session aggregate rules (SQL + Recursive CTEs on closed session):**

| Rule | Logic |
|---|---|
| Impossible travel | Two sessions for same user, different countries, `time_diff < travel_threshold` — classic recursive CTE across `sessions` ordered by `start_time` |
| Excessive downloads | `COUNT(attachment_download)` within session > baseline percentile |
| Session duration outlier | `duration_sec` > μ + 3σ of user's historical sessions |
| Off-hours activity | `start_time_utc` outside user's normal working hours pattern |
| Account sharing | Same `user_id`, overlapping session time windows, different `device_id` |

Recursive CTE example pattern (impossible travel):
```sql
WITH RECURSIVE session_pairs AS (
  SELECT s1.session_id, s1.user_id, s1.location_id, s1.start_time_utc,
         s2.session_id AS next_session_id, s2.location_id AS next_location_id,
         s2.start_time_utc AS next_start
  FROM sessions s1
  JOIN sessions s2 ON s1.user_id = s2.user_id
    AND s2.start_time_utc > s1.start_time_utc
    AND s2.start_time_utc - s1.end_time_utc < INTERVAL '3 hours'
  JOIN locations l1 ON l1.location_id = s1.location_id
  JOIN locations l2 ON l2.location_id = s2.location_id
  WHERE l1.country_code <> l2.country_code
)
SELECT * FROM session_pairs;
```

TimescaleDB's continuous aggregates are used for the baseline computation (mean/stddev of session durations, download counts per user).


## 5. Alerting After Detection

Alerts are published to a dedicated **Kafka alert topic** (`security.alerts`). Each alert event contains:
- `alert_id`, `alert_type`, `severity` (LOW/MEDIUM/HIGH/CRITICAL)
- `session_id`, `user_id`
- `triggered_at`
- `evidence` JSONB — links back to `event_id` list and `raw_event_id` list for full traceability
- `detection_mode` (STREAMING | POST_SESSION)

**Alert consumers:**
- **Kafbat UI** — displays the alert topic in real time, acts as the lightweight operations dashboard; operators can inspect the raw Kafka message including the evidence chain
- **DB writer** — consumes alerts and writes to `alerts` table in PostgreSQL for historical querying and correlation
- Optionally: downstream SIEM or ticketing webhook consumer

The evidence JSONB in each alert enables the full drill-down chain: Alert → Session → Normalized Events → Raw Events → Original Log Line.


## 6. Technical Architecture

**All infrastructure runs as Docker containers:**

| Component | Role |
|---|---|
| PostgreSQL + TimescaleDB | Single DB for master data, raw events, normalized events, sessions, alerts |
| Kafka (KRaft mode, no ZooKeeper) | Message broker for raw log streams and alert events |
| Kafbat | Web UI for Kafka topic inspection and alert dashboard |
| Spring Boot Pipeline Service | Kafka consumer/producer pipeline, detection, alerting |

**Spring Boot application structure:**

| Module / Layer | Responsibility |
|---|---|
| `spring-kafka` | `@KafkaListener` consumers on the four input topics; `KafkaTemplate` for alert publishing |
| `spring-data-jpa` + `HikariCP` | Master data access (users, devices, locations, applications) |
| `spring-jdbc` / `JdbcTemplate` | Raw and normalized event writes; CTE-based detection queries (JPA is too thin for recursive CTEs) |
| `spring-scheduling` (`@Scheduled`) | Post-session aggregate detection job — runs CTE queries on closed sessions at a configurable interval |
| `application.yml` | Kafka broker URLs, topic names, DB connection, detection thresholds, scheduling intervals — all externalized |

**Key Spring Boot design decisions:**
- **Four `@KafkaListener` methods**, one per input topic (`logs.email`, `logs.idp`, `logs.network`, `logs.geolocation`), each deserializing into a typed raw event POJO before the shared normalization pipeline
- **`@Transactional` per event** — raw event write and normalized event write happen atomically; entity resolution reads from the master data cache
- **In-memory entity cache** (`@Cacheable` via Spring Cache + Caffeine) for master data lookups (users, devices, known locations) to avoid a DB round-trip per event
- **`JdbcTemplate` for CTEs** — post-session detection uses named `JdbcTemplate` queries with recursive CTEs; results mapped to alert POJOs and published to `security.alerts` via `KafkaTemplate`
- **`@Scheduled` post-session job** — configurable fixed-delay (e.g., every 60 seconds); queries sessions with `status = 'CLOSED'` and `processed = false`, runs aggregate detection, marks session as processed
- **Docker Compose** includes the Spring Boot service alongside Kafka, Kafbat, and PostgreSQL+TimescaleDB; environment variables override `application.yml` for the container context


**Data flow by source type:**
- **Master data** — loaded once at startup as SQL seed scripts (users, devices, known locations, applications); this represents the MDM / entity resolution reference data
- **Log streams** — produced to Kafka topics (`logs.email`, `logs.idp`, `logs.network`, `logs.geolocation`) by log producers (simulated in test mode); consumed by the Spring Boot pipeline
- **Alert events** — published by the Spring Boot pipeline to `security.alerts`; consumed by Kafbat UI and the DB alert writer

**Spring Boot pipeline responsibilities:**
- Multi-topic Kafka consumer (one consumer group, four input topics via `@KafkaListener`)
- Per-event: parse → normalize → resolve entity (lookup against master data, served from Caffeine cache) → enrich (GeoIP, risk score) → write `raw_event` + `normalized_event` → run in-session detection rules → publish alert via `KafkaTemplate` if triggered
- `@Scheduled` post-session job: query closed sessions → run CTE-based aggregate rules via `JdbcTemplate` → publish alerts


## 7. Test Data Generation

**Normal sessions (baseline behavior):**
- `john.doe` logs in from Berlin, corporate laptop, business hours (08:00–18:00 CET)
- Reads 10–30 emails, downloads 0–3 attachments, logs out cleanly
- Consistent device, consistent IP range, no external SMTP

**Attack scenarios to generate:**

| Scenario | Generated Pattern |
|---|---|
| Impossible travel | Session from Berlin at 09:00, session from Bangkok at 10:30, same `user_id` |
| Session hijacking | Session starts on known laptop, mid-session events switch to unknown `device_id` and new IP |
| Data exfiltration | Session with 200+ attachment downloads and external SMTP connections to non-allowlisted relay |
| Brute force | 10+ failed IdP auth events in 2-minute window before successful login |
| Off-hours access | Login at 03:00 UTC from home IP, short session, bulk download |
| Account sharing | Two concurrent sessions, same `user_id`, different devices and IPs, overlapping time window |

The test data generator produces realistic JSONB log payloads with proper timestamps, produces them to the Kafka input topics at configurable rates, and includes known-good baseline sessions to ensure the detection rules have a stable signal-to-noise ratio to evaluate against.
