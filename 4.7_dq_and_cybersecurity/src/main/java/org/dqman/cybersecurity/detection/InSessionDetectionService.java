package org.dqman.cybersecurity.detection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dqman.cybersecurity.alert.AlertPublisher;
import org.dqman.cybersecurity.model.Alert;
import org.dqman.cybersecurity.model.KnownLocation;
import org.dqman.cybersecurity.model.RawLogEvent;
import org.dqman.cybersecurity.model.Session;
import org.dqman.cybersecurity.model.User;
import org.dqman.cybersecurity.pipeline.EntityResolutionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * In-session detection rules evaluated per event as it arrives.
 * Each rule checks a single invariant and publishes an alert via KafkaTemplate
 * if the invariant is violated.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InSessionDetectionService {

    private final EntityResolutionService entityResolution;
    private final AlertPublisher alertPublisher;
    private final JdbcTemplate jdbc;

    @Value("${app.detection.failed-login-window-minutes}")
    private int failedLoginWindowMinutes;

    @Value("${app.detection.failed-login-threshold}")
    private int failedLoginThreshold;

    @Value("${app.detection.smtp-allowlist}")
    private List<String> smtpAllowlist;

    /**
     * Runs all in-session detection rules against a single event.
     * Rules are independent and all are evaluated — no short-circuit on first hit.
     *
     * @param event             the raw event being processed
     * @param normalizedEventId ID of the corresponding normalized_events record (for evidence chain)
     * @param session           the session this event belongs to
     * @param user              resolved user entity, may be {@code null} for unknown usernames
     * @param location          resolved known location for the event IP, may be {@code null}
     */
    public void evaluate(RawLogEvent event, String normalizedEventId,
                         Session session, User user, KnownLocation location) {
        checkNewDevice(event, normalizedEventId, session, user);
        checkIpChangeMidSession(event, normalizedEventId, session);
        checkNewCountry(event, normalizedEventId, session, user, location);
        checkFailedLoginSpike(event, normalizedEventId, session);
        checkSuspiciousSmtp(event, normalizedEventId, session);
    }

    /**
     * Rule: Current event device_id ∉ user's known devices (from MDM master data).
     */
    private void checkNewDevice(RawLogEvent event, String normalizedEventId,
                                Session session, User user) {
        if (user == null || event.getDeviceId() == null) return;
        if (!entityResolution.isKnownDevice(user.getId(), event.getDeviceId())) {
            publish(Alert.builder()
                    .alertType("NEW_DEVICE")
                    .severity("MEDIUM")
                    .sessionId(session.getSessionId())
                    .userId(user.getId())
                    .triggeredAt(Instant.now())
                    .detectionMode("STREAMING")
                    .evidence(Map.of(
                            "eventIds", List.of(normalizedEventId),
                            "rawEventIds", List.of(event.getEventId()),
                            "device_id", event.getDeviceId(),
                            "details", "Device not in user's MDM-registered device list"
                    ))
                    .build());
        }
    }

    /**
     * Rule: Event IP ≠ session login IP (session context initial_ip).
     */
    private void checkIpChangeMidSession(RawLogEvent event, String normalizedEventId, Session session) {
        if (event.getIp() == null || session.getContext() == null) return;
        String initialIp = (String) session.getContext().get("initial_ip");
        if (initialIp != null && !initialIp.equals(event.getIp())) {
            publish(Alert.builder()
                    .alertType("IP_CHANGE_MID_SESSION")
                    .severity("HIGH")
                    .sessionId(session.getSessionId())
                    .userId(session.getUserId())
                    .triggeredAt(Instant.now())
                    .detectionMode("STREAMING")
                    .evidence(Map.of(
                            "eventIds", List.of(normalizedEventId),
                            "rawEventIds", List.of(event.getEventId()),
                            "initial_ip", initialIp,
                            "current_ip", event.getIp(),
                            "details", "IP changed within active session — possible session hijacking"
                    ))
                    .build());
        }
    }

    /**
     * Rule: GeoIP country ≠ user's home country.
     */
    private void checkNewCountry(RawLogEvent event, String normalizedEventId,
                                 Session session, User user, KnownLocation location) {
        if (user == null || location == null) return;
        if (!location.getCountryCode().equals(user.getHomeCountryCode())) {
            publish(Alert.builder()
                    .alertType("NEW_COUNTRY")
                    .severity("HIGH")
                    .sessionId(session.getSessionId())
                    .userId(user.getId())
                    .triggeredAt(Instant.now())
                    .detectionMode("STREAMING")
                    .evidence(Map.of(
                            "eventIds", List.of(normalizedEventId),
                            "rawEventIds", List.of(event.getEventId()),
                            "home_country", user.getHomeCountryCode(),
                            "event_country", location.getCountryCode(),
                            "details", "Login from country outside user's home country"
                    ))
                    .build());
        }
    }

    /**
     * Rule: ≥ N failed auth events within the configured time window.
     */
    private void checkFailedLoginSpike(RawLogEvent event, String normalizedEventId, Session session) {
        if (!"IDP".equals(event.getSourceSystem())) return;
        Object authStatus = event.getPayload() != null ? event.getPayload().get("auth_status") : null;
        if (!"FAILED".equals(authStatus)) return;

        Instant windowStart = Instant.now().minus(failedLoginWindowMinutes, ChronoUnit.MINUTES);
        Integer failCount = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM normalized_events
                WHERE session_id = ?
                  AND payload->>'auth_status' = 'FAILED'
                  AND event_time_utc >= ?
                """,
                Integer.class,
                session.getSessionId(),
                java.sql.Timestamp.from(windowStart)
        );

        if (failCount != null && failCount >= failedLoginThreshold) {
            publish(Alert.builder()
                    .alertType("FAILED_LOGIN_SPIKE")
                    .severity("CRITICAL")
                    .sessionId(session.getSessionId())
                    .userId(session.getUserId())
                    .triggeredAt(Instant.now())
                    .detectionMode("STREAMING")
                    .evidence(Map.of(
                            "eventIds", List.of(normalizedEventId),
                            "rawEventIds", List.of(event.getEventId()),
                            "fail_count", failCount,
                            "window_minutes", failedLoginWindowMinutes,
                            "details", "Brute-force: " + failCount + " failed logins in " + failedLoginWindowMinutes + " minutes"
                    ))
                    .build());
        }
    }

    /**
     * Rule: Network event to non-allowlisted external SMTP relay.
     */
    private void checkSuspiciousSmtp(RawLogEvent event, String normalizedEventId, Session session) {
        if (!"NETWORK".equals(event.getSourceSystem())) return;
        Object dest = event.getPayload() != null ? event.getPayload().get("smtp_destination") : null;
        if (dest == null) return;

        String destination = dest.toString();
        boolean allowed = smtpAllowlist.stream().anyMatch(destination::contains);
        if (!allowed) {
            publish(Alert.builder()
                    .alertType("SUSPICIOUS_SMTP_RELAY")
                    .severity("HIGH")
                    .sessionId(session.getSessionId())
                    .userId(session.getUserId())
                    .triggeredAt(Instant.now())
                    .detectionMode("STREAMING")
                    .evidence(Map.of(
                            "eventIds", List.of(normalizedEventId),
                            "rawEventIds", List.of(event.getEventId()),
                            "smtp_destination", destination,
                            "details", "SMTP relay to non-allowlisted external destination"
                    ))
                    .build());
        }
    }

    private void publish(Alert alert) {
        log.warn("ALERT [{}] severity={} session={}", alert.getAlertType(), alert.getSeverity(), alert.getSessionId());
        alertPublisher.publish(alert);
    }
}
