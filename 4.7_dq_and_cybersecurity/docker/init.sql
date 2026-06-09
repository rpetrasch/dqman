-- ============================================================
-- Email Session Security — Database Schema
-- PostgreSQL + TimescaleDB
-- ============================================================

CREATE EXTENSION IF NOT EXISTS timescaledb;

-- ============================================================
-- MASTER DATA (static, entity resolution reference)
-- ============================================================

CREATE TABLE users (
    id                BIGSERIAL PRIMARY KEY,
    username          VARCHAR(100) UNIQUE NOT NULL,
    email             VARCHAR(255),
    home_country_code CHAR(2),
    normal_work_start VARCHAR(5),   -- e.g. '08:00'
    normal_work_end   VARCHAR(5),   -- e.g. '18:00'
    home_timezone     VARCHAR(50)
);

CREATE TABLE devices (
    id          BIGSERIAL PRIMARY KEY,
    device_id   VARCHAR(100) UNIQUE NOT NULL,
    hostname    VARCHAR(255),
    trust_level VARCHAR(20),        -- CORPORATE, PERSONAL, UNKNOWN
    owner_id    BIGINT REFERENCES users(id)
);

CREATE TABLE known_locations (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100),
    location_type VARCHAR(20),      -- OFFICE, HOME, VPN
    country_code  CHAR(2),
    ip_cidr       VARCHAR(50),
    latitude      DOUBLE PRECISION,
    longitude     DOUBLE PRECISION
);

CREATE TABLE applications (
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(100),
    app_type VARCHAR(30),           -- EMAIL_CLIENT, IDP, NETWORK, GEOLOCATION
    version  VARCHAR(20)
);

-- ============================================================
-- SESSION-CENTRIC MODEL
-- ============================================================

CREATE TABLE sessions (
    session_id     VARCHAR(100) PRIMARY KEY,
    user_id        BIGINT REFERENCES users(id),
    start_time_utc TIMESTAMPTZ,
    end_time_utc   TIMESTAMPTZ,
    duration_sec   BIGINT,
    status         VARCHAR(20) DEFAULT 'OPEN',   -- OPEN, CLOSED
    risk_score     DOUBLE PRECISION DEFAULT 0.0,
    processed      BOOLEAN DEFAULT FALSE,
    context        JSONB        -- initial IP, device, country at login time
);

-- ============================================================
-- EVENT STORE (TimescaleDB hypertables)
-- Three timestamps per event: event_time, ingestion_time, processing_time
-- ============================================================

-- Immutable raw log store — full forensic evidence
CREATE TABLE raw_events (
    event_id       VARCHAR(100) NOT NULL,
    source_system  VARCHAR(50)  NOT NULL,   -- EMAIL, IDP, NETWORK, GEOLOCATION
    source_file    VARCHAR(500),
    source_offset  BIGINT,
    event_time_utc TIMESTAMPTZ  NOT NULL,   -- when it happened (source clock, UTC)
    ingestion_time TIMESTAMPTZ  NOT NULL,   -- when Kafka received it
    payload        JSONB        NOT NULL,   -- original log line preserved as-is
    PRIMARY KEY (event_id, ingestion_time)
);
SELECT create_hypertable('raw_events', 'ingestion_time');

-- Normalized and enriched events — correlation target
CREATE TABLE normalized_events (
    event_id          VARCHAR(100) NOT NULL,
    raw_event_id      VARCHAR(100) NOT NULL,  -- FK → raw_events (lineage)
    session_id        VARCHAR(100) REFERENCES sessions(session_id),
    user_id           BIGINT       REFERENCES users(id),
    event_time_utc    TIMESTAMPTZ  NOT NULL,  -- source clock, UTC normalized
    ingestion_time    TIMESTAMPTZ,            -- when Kafka received the raw event
    processing_time   TIMESTAMPTZ,            -- when pipeline wrote to DB
    event_type        VARCHAR(50),            -- LOGIN, EMAIL_READ, ATTACHMENT_DOWNLOAD, SMTP_RELAY, etc.
    device_id         VARCHAR(100),
    ip                VARCHAR(50),
    location_id       BIGINT       REFERENCES known_locations(id),
    session_offset_sec BIGINT,               -- seconds since session start
    payload           JSONB,                  -- enriched attributes (varies by event_type)
    PRIMARY KEY (event_id, event_time_utc)
);
SELECT create_hypertable('normalized_events', 'event_time_utc');

-- ============================================================
-- RELATIONSHIP GRAPH EDGES (recursive CTE target)
-- ============================================================

CREATE TABLE entity_edges (
    id                BIGSERIAL PRIMARY KEY,
    source_type       VARCHAR(50),    -- USER, DEVICE, LOCATION, SESSION
    source_id         VARCHAR(100),
    target_type       VARCHAR(50),
    target_id         VARCHAR(100),
    relationship_type VARCHAR(50),    -- OWNS, LOGGED_IN_FROM, CONNECTED_TO
    valid_from        TIMESTAMPTZ,
    valid_to          TIMESTAMPTZ
);

-- ============================================================
-- ALERTS
-- Full drill-down chain: Alert → Session → Normalized Events → Raw Events
-- ============================================================

CREATE TABLE alerts (
    alert_id       VARCHAR(100) PRIMARY KEY,
    alert_type     VARCHAR(100),
    severity       VARCHAR(20),      -- LOW, MEDIUM, HIGH, CRITICAL
    session_id     VARCHAR(100)  REFERENCES sessions(session_id),
    user_id        BIGINT        REFERENCES users(id),
    triggered_at   TIMESTAMPTZ,
    detection_mode VARCHAR(20),      -- STREAMING, POST_SESSION
    evidence       JSONB             -- event_id list + raw_event_id list for traceability
);

-- ============================================================
-- INDEXES for detection queries
-- ============================================================

CREATE INDEX idx_normalized_events_session   ON normalized_events (session_id, event_time_utc DESC);
CREATE INDEX idx_normalized_events_user      ON normalized_events (user_id, event_time_utc DESC);
CREATE INDEX idx_normalized_events_type      ON normalized_events (event_type, event_time_utc DESC);
CREATE INDEX idx_sessions_user_status        ON sessions (user_id, status, start_time_utc DESC);
CREATE INDEX idx_entity_edges_source         ON entity_edges (source_type, source_id);
CREATE INDEX idx_alerts_session              ON alerts (session_id, triggered_at DESC);

-- ============================================================
-- MASTER DATA SEED
-- ============================================================

INSERT INTO users (username, email, home_country_code, normal_work_start, normal_work_end, home_timezone) VALUES
('john.doe',   'john.doe@corp.example.com',   'DE', '08:00', '18:00', 'Europe/Berlin'),
('jane.smith', 'jane.smith@corp.example.com', 'DE', '09:00', '17:00', 'Europe/Berlin'),
('admin.svc',  'admin.svc@corp.example.com',  'DE', '00:00', '23:59', 'UTC');

INSERT INTO devices (device_id, hostname, trust_level, owner_id) VALUES
('CORP-LAPTOP-001',  'john-laptop-corp',   'CORPORATE', 1),
('CORP-LAPTOP-002',  'jane-laptop-corp',   'CORPORATE', 2),
('PERSONAL-MOB-001', 'john-iphone',        'PERSONAL',  1),
('UNKNOWN-DEV-999',  'unknown-host',       'UNKNOWN',   NULL);

INSERT INTO known_locations (name, location_type, country_code, ip_cidr, latitude, longitude) VALUES
('Berlin HQ',          'OFFICE', 'DE', '10.0.0.0/8',      52.5200, 13.4050),
('John Home Office',   'HOME',   'DE', '192.168.1.0/24',  52.4900, 13.3800),
('Corporate VPN',      'VPN',    'DE', '172.16.0.0/12',   0,       0),
('Bangkok DC',         'OFFICE', 'TH', '203.150.0.0/16',  13.7563, 100.5018);

INSERT INTO applications (name, app_type, version) VALUES
('Microsoft Outlook', 'EMAIL_CLIENT', '16.0'),
('Azure AD',          'IDP',          '2.0'),
('Palo Alto NGFW',    'NETWORK',      '10.2'),
('Corporate MDM',     'GEOLOCATION',  '1.0');

-- ============================================================
-- TimescaleDB continuous aggregate for session baselines
-- (mean/stddev of session durations and download counts per user)
-- ============================================================

CREATE MATERIALIZED VIEW user_session_baselines
WITH (timescaledb.continuous) AS
SELECT
    user_id,
    time_bucket('7 days', event_time_utc) AS bucket,
    COUNT(*)                               AS event_count,
    COUNT(*) FILTER (WHERE event_type = 'ATTACHMENT_DOWNLOAD') AS download_count
FROM normalized_events
GROUP BY user_id, bucket
WITH NO DATA;
