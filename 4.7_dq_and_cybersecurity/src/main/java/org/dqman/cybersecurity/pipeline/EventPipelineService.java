package org.dqman.cybersecurity.pipeline;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dqman.cybersecurity.detection.InSessionDetectionService;
import org.dqman.cybersecurity.model.*;
import org.dqman.cybersecurity.repository.SessionRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates the full per-event pipeline:
 * parse → normalize → entity resolution → enrich → persist → in-session detection
 *
 * The @Transactional boundary ensures raw_event + normalized_event writes are atomic:
 * either both land in the DB or neither does, preserving lineage integrity.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPipelineService {

    private final EntityResolutionService entityResolution;
    private final InSessionDetectionService inSessionDetector;
    private final SessionRepository sessionRepository;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    /**
     * Processes a single raw log event through the full pipeline.
     * <p>
     * Steps executed within one transaction:
     * <ol>
     *   <li>Persist the immutable raw event (full original payload as JSONB)</li>
     *   <li>Resolve user and location from Caffeine-cached master data</li>
     *   <li>Derive {@code event_type} and incremental risk score</li>
     *   <li>Resolve or create the enclosing session</li>
     *   <li>Persist the normalized event with all three timestamps and the
     *       {@code raw_event_id} lineage FK</li>
     *   <li>Evaluate in-session detection rules; publish alerts if triggered</li>
     * </ol>
     *
     * @param event the deserialized Kafka message; {@code ingestionTime} and
     *              {@code sourceOffset} must be set by the caller before this method
     */
    @Transactional
    public void process(RawLogEvent event) {
        Instant processingTime = Instant.now();

        // 1. Persist immutable raw event — full forensic evidence preserved
        persistRawEvent(event, processingTime);

        // 2. Entity resolution via Caffeine-cached master data lookups
        User user = entityResolution.resolveUser(event.getUsername()).orElse(null);
        KnownLocation location = (event.getIp() != null)
                ? entityResolution.resolveLocation(event.getIp()).orElse(null)
                : null;

        // 3. Enrich: derive event_type, risk scoring
        String eventType = deriveEventType(event);
        double riskDelta = scoreRisk(event, user, location);

        // 4. Resolve or create session
        Session session = resolveOrCreateSession(event, user, location);
        session.setRiskScore(session.getRiskScore() + riskDelta);
        long sessionOffsetSec = session.getStartTimeUtc() != null
                ? Instant.now().getEpochSecond() - session.getStartTimeUtc().getEpochSecond()
                : 0;

        // Close session on LOGOUT so post-session detection can evaluate it
        if (isLogoutEvent(event)) {
            session.setStatus("CLOSED");
            session.setEndTimeUtc(event.getEventTimeUtc());
            if (session.getStartTimeUtc() != null) {
                session.setDurationSec(ChronoUnit.SECONDS.between(session.getStartTimeUtc(), event.getEventTimeUtc()));
            }
        }

        sessionRepository.saveAndFlush(session);

        // 5. Persist normalized event with all three timestamps and lineage FK
        String normalizedEventId = UUID.randomUUID().toString();
        persistNormalizedEvent(normalizedEventId, event, session, user, location,
                eventType, sessionOffsetSec, processingTime);

        // 6. In-session detection rules — fire alerts if triggered
        inSessionDetector.evaluate(event, normalizedEventId, session, user, location);

        log.debug("Processed {} event {} for session {}", event.getSourceSystem(), event.getEventId(), session.getSessionId());
    }

    /**
     * Writes the immutable raw event record to the {@code raw_events} TimescaleDB hypertable.
     * {@code ON CONFLICT DO NOTHING} makes the write idempotent for at-least-once delivery.
     */
    private void persistRawEvent(RawLogEvent event, Instant processingTime) {
        String payloadJson = toJson(event.getPayload());
        jdbc.update(
                """
                INSERT INTO raw_events
                    (event_id, source_system, source_file, source_offset, event_time_utc, ingestion_time, payload)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
                ON CONFLICT (event_id, ingestion_time) DO NOTHING
                """,
                event.getEventId(),
                event.getSourceSystem(),
                event.getSourceFile(),
                event.getSourceOffset(),
                Timestamp.from(event.getEventTimeUtc()),
                Timestamp.from(event.getIngestionTime()),
                payloadJson
        );
    }

    /**
     * Writes the enriched, normalized event to the {@code normalized_events} hypertable.
     * The {@code raw_event_id} FK closes the lineage chain back to the original log line.
     * The enriched payload JSONB adds resolved country and home-country fields on top of
     * the original payload, without modifying the immutable {@code raw_events} record.
     */
    private void persistNormalizedEvent(String normalizedEventId, RawLogEvent event,
                                        Session session, User user, KnownLocation location,
                                        String eventType, long sessionOffsetSec, Instant processingTime) {
        Map<String, Object> enrichedPayload = new HashMap<>(event.getPayload() != null ? event.getPayload() : Map.of());
        if (location != null) {
            enrichedPayload.put("resolved_country", location.getCountryCode());
            enrichedPayload.put("resolved_location_name", location.getName());
        }
        if (user != null) {
            enrichedPayload.put("user_home_country", user.getHomeCountryCode());
        }

        jdbc.update(
                """
                INSERT INTO normalized_events
                    (event_id, raw_event_id, session_id, user_id, event_time_utc, ingestion_time,
                     processing_time, event_type, device_id, ip, location_id, session_offset_sec, payload)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                ON CONFLICT (event_id, event_time_utc) DO NOTHING
                """,
                normalizedEventId,
                event.getEventId(),
                session.getSessionId(),
                user != null ? user.getId() : null,
                Timestamp.from(event.getEventTimeUtc()),
                Timestamp.from(event.getIngestionTime()),
                Timestamp.from(processingTime),
                eventType,
                event.getDeviceId(),
                event.getIp(),
                location != null ? location.getId() : null,
                sessionOffsetSec,
                toJson(enrichedPayload)
        );
    }

    /**
     * Looks up an existing session by {@code sessionId} from the event, or creates a new
     * session record if this is the first event for that ID.
     */
    private Session resolveOrCreateSession(RawLogEvent event, User user, KnownLocation location) {
        if (event.getSessionId() != null) {
            return sessionRepository.findById(event.getSessionId()).orElseGet(() -> createSession(event, user, location));
        }
        return createSession(event, user, location);
    }

    /**
     * Creates a new session record, capturing the login-time context (IP, device, country)
     * in a JSONB bag.  This snapshot is the reference point for mid-session IP-change and
     * impossible-travel detection.
     */
    private Session createSession(RawLogEvent event, User user, KnownLocation location) {
        Session session = new Session();
        session.setSessionId(event.getSessionId() != null ? event.getSessionId() : UUID.randomUUID().toString());
        session.setUserId(user != null ? user.getId() : null);
        session.setStartTimeUtc(event.getEventTimeUtc());
        session.setStatus("OPEN");
        session.setRiskScore(0.0);
        session.setProcessed(false);

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("initial_ip", event.getIp());
        ctx.put("initial_device_id", event.getDeviceId());
        ctx.put("country_code", location != null ? location.getCountryCode() : "UNKNOWN");
        session.setContext(ctx);

        return session;
    }

    /**
     * Derives a canonical {@code event_type} string.  Prefers an explicit {@code event_type}
     * field in the payload (set by the producer) over the source-system fallback, so that
     * fine-grained types such as {@code ATTACHMENT_DOWNLOAD} survive into normalized_events.
     */
    private String deriveEventType(RawLogEvent event) {
        Object type = event.getPayload() != null ? event.getPayload().get("event_type") : null;
        if (type != null) return type.toString();
        return switch (event.getSourceSystem()) {
            case "EMAIL"       -> "EMAIL_EVENT";
            case "IDP"         -> "AUTH_EVENT";
            case "NETWORK"     -> "NETWORK_EVENT";
            case "GEOLOCATION" -> "GEO_EVENT";
            default            -> "UNKNOWN";
        };
    }

    /**
     * Computes an incremental risk score delta for this event.
     * The score is additive: each session accumulates risk across its events.
     * Weights are intentionally simple for readability — production systems
     * would use a trained model or a richer rule set.
     *
     * @return a value in [0.0, 1.0] to be added to the running session risk score
     */
    private double scoreRisk(RawLogEvent event, User user, KnownLocation location) {
        double score = 0.0;
        if (location == null) score += 0.2;
        if (user == null) score += 0.3;
        Object status = event.getPayload() != null ? event.getPayload().get("auth_status") : null;
        if ("FAILED".equals(status)) score += 0.1;
        return score;
    }

    private boolean isLogoutEvent(RawLogEvent event) {
        return event.getPayload() != null && "LOGOUT".equals(event.getPayload().get("event_type"));
    }

    private String toJson(Object obj) {
        if (obj == null) return "{}";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
