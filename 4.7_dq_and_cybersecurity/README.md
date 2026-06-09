# 4.7 — Data Quality & Cybersecurity

A practical example showing how **data quality principles are the foundation of
security detection**.  The scenario is a corporate email user (`john.doe`) whose
sessions are monitored across four log sources in real time and post-session.

> For the full technical reference see [Implementation.md](Implementation.md).
> For the design rationale see [Spec.md](Spec.md).

---

## What This Example Shows

| DQ Principle | Security Application |
|---|---|
| **Lineage traceability** | Every alert links back: Alert → Session → Normalized Event → Raw Event → original log line |
| **Three-timestamp model** | `event_time`, `ingestion_time`, `processing_time` per event — exposes clock skew and forensic timeline gaps |
| **Immutability** | Raw events are write-once; enrichment never overwrites evidence |
| **Entity resolution** | Raw usernames and IPs are resolved to canonical master data before any analysis |
| **Master data separation** | Users, devices, locations loaded once; not duplicated per event |
| **Outlier detection** | TimescaleDB continuous aggregates compute rolling μ/σ baselines for anomaly scoring |

The pipeline handles six attack scenarios defined in the spec:
impossible travel, session hijacking, data exfiltration, brute-force login,
off-hours access, and account sharing — alongside a normal-behaviour baseline.

---

## Prerequisites

| Tool | Version |
|---|---|
| Java | 24+ |
| Maven | 3.9+ |
| Docker + Docker Compose | 24+ |

---

## Quick Start

```bash
# 1. Build the Spring Boot fat JAR (from the module root)
mvn package -DskipTests

# 2. Start all infrastructure and the pipeline service
cd docker
docker compose up -d

# 3. Watch the alert stream in Kafbat UI
open http://localhost:8080
# → Topics → security.alerts → Messages

# 4. Query the database directly (optional)
psql -h localhost -U security_user -d security_db
```

The test data generator starts automatically and cycles through all attack
scenarios every 8 seconds.  You should see alerts appearing in the Kafbat UI
within the first minute.

---

## Architecture

```
[Email Logs]  [IdP Logs]  [Network Logs]  [Device GeoIP]
      │              │              │               │
      └──────────────┴──────────────┴───────────────┘
                            │
                     [Kafka Topics]
                     logs.email / logs.idp
                     logs.network / logs.geolocation
                            │
               [Spring Boot Pipeline Service]
               parse → normalize → resolve → enrich
               → persist (raw + normalized) → detect
                            │
                  [PostgreSQL + TimescaleDB]
                  master data · raw events (immutable)
                  normalized events · sessions · alerts
                            │
             ┌──────────────┴──────────────┐
     [Streaming rules]          [Post-session CTEs]
     per-event (in-session)     @Scheduled on closed sessions
             │                             │
             └──────────────┬──────────────┘
                            │
                  [Kafka: security.alerts]
                            │
               ┌────────────┴────────────┐
          [Kafbat UI]              [alerts table]
          real-time dashboard      historical queries
```

---

## Services

| Service | URL / Port | Purpose |
|---|---|---|
| Kafbat UI | [http://localhost:8080](http://localhost:8080) | Browse Kafka topics and inspect alert messages |
| Pipeline API | [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health) | Spring Boot health endpoint |
| PostgreSQL | `localhost:5432` | Direct DB access (user: `security_user`, db: `security_db`) |
| Kafka | `localhost:9092` | Kafka broker (KRaft mode, no ZooKeeper) |

---

## Detection Rules

### Streaming (per-event, real-time)

| Alert | Trigger |
|---|---|
| `NEW_DEVICE` | Login from a device not in the user's MDM record |
| `IP_CHANGE_MID_SESSION` | Event IP differs from the session login IP |
| `NEW_COUNTRY` | GeoIP country differs from the user's home country |
| `FAILED_LOGIN_SPIKE` | ≥ 3 failed auth events within a 5-minute window |
| `SUSPICIOUS_SMTP_RELAY` | SMTP connection to a non-allowlisted external relay |

### Post-Session (aggregate, CTE-based)

| Alert | Trigger |
|---|---|
| `IMPOSSIBLE_TRAVEL` | Two sessions, same user, different countries, gap < 3 hours |
| `EXCESSIVE_DOWNLOADS` | More than 50 attachment downloads in one session |
| `SESSION_DURATION_OUTLIER` | Session length exceeds μ + 3σ of the user's history |
| `OFF_HOURS_ACTIVITY` | Events outside the user's configured working hours |
| `ACCOUNT_SHARING` | Concurrent sessions on different devices for the same user |

---

## Inspecting Alerts

Each alert in the `security.alerts` Kafka topic is a JSON message with a full
evidence chain:

```json
{
  "alertId": "3fa85f64-...",
  "alertType": "IMPOSSIBLE_TRAVEL",
  "severity": "CRITICAL",
  "sessionId": "SESSION-TRAVEL-BER-abc12345",
  "userId": 1,
  "triggeredAt": "2026-06-04T09:32:00Z",
  "detectionMode": "POST_SESSION",
  "evidence": {
    "session_1": "SESSION-TRAVEL-BER-abc12345",
    "session_2": "SESSION-TRAVEL-BKK-def67890",
    "country_1": "DE",
    "country_2": "TH",
    "gap_hours": 1.5,
    "details": "Country changed DE→TH with only 1.5 hours gap"
  }
}
```

Follow the evidence chain in the database:

```sql
-- 1. Find the alert
SELECT * FROM alerts WHERE alert_type = 'IMPOSSIBLE_TRAVEL' ORDER BY triggered_at DESC LIMIT 5;

-- 2. Inspect the session context (login-time IP, device, country)
SELECT session_id, context, risk_score FROM sessions WHERE session_id = '<session_id>';

-- 3. List all events in the session
SELECT event_time_utc, event_type, ip, device_id, payload
FROM normalized_events
WHERE session_id = '<session_id>'
ORDER BY event_time_utc;

-- 4. Drill down to the original raw log line
SELECT source_system, source_file, source_offset, event_time_utc, payload
FROM raw_events
WHERE event_id = '<raw_event_id>';
```

---

## Project Structure

```
4.7_dq_and_cybersecurity/
├── Spec.md                          Design specification
├── Implementation.md                Full technical reference
├── pom.xml                          Maven module descriptor
├── docker/
│   ├── docker-compose.yml           All four containers
│   ├── init.sql                     Schema, hypertables, seed master data
│   └── Dockerfile                   Spring Boot container image
└── src/main/
    ├── resources/
    │   └── application.yml          All thresholds and topic names externalized
    └── java/org/dqman/cybersecurity/
        ├── Main.java
        ├── model/                   JPA entities (master data) + POJOs (Kafka events)
        ├── repository/              Spring Data JPA repositories
        ├── consumer/                EventConsumer — 4 @KafkaListener methods
        ├── pipeline/                EntityResolutionService · EventPipelineService
        ├── detection/               InSessionDetectionService · PostSessionDetectionService
        ├── alert/                   AlertPublisher → security.alerts
        └── generator/               TestDataGenerator — 6 attack scenarios
```

---

## Configuration

Key properties in `application.yml` (all overridable via environment variables):

```yaml
app:
  detection:
    failed-login-threshold: 3          # brute-force: N failures in window
    failed-login-window-minutes: 5
    excessive-download-threshold: 50   # exfiltration: downloads per session
    impossible-travel-hours: 3         # travel threshold
    smtp-allowlist: smtp.corp.example.com,mail.corp.example.com  # comma-separated
  generator:
    enabled: true                      # set false to disable test data
    interval-ms: 8000
```

Docker Compose passes `SPRING_DATASOURCE_URL` and `SPRING_KAFKA_BOOTSTRAP_SERVERS`
as environment variables, so the same JAR runs locally (pointing at `localhost`)
and inside the container network (pointing at service names).
