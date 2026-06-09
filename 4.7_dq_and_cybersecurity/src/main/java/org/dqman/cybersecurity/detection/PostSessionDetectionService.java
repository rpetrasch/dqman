package org.dqman.cybersecurity.detection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dqman.cybersecurity.alert.AlertPublisher;
import org.dqman.cybersecurity.model.Alert;
import org.dqman.cybersecurity.model.Session;
import org.dqman.cybersecurity.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Post-session aggregate detection using SQL + Recursive CTEs on closed sessions.
 * Runs on a fixed-delay schedule; finds unprocessed closed sessions, evaluates
 * all aggregate rules, marks sessions as processed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostSessionDetectionService {

    private final SessionRepository sessionRepository;
    private final AlertPublisher alertPublisher;
    private final JdbcTemplate jdbc;

    @Value("${app.detection.impossible-travel-hours}")
    private int impossibleTravelHours;

    @Value("${app.detection.excessive-download-threshold}")
    private int excessiveDownloadThreshold;

    @Value("${app.detection.session-duration-sigma}")
    private double sessionDurationSigma;

    @Value("${app.detection.off-hours-start}")
    private String offHoursStart;

    @Value("${app.detection.off-hours-end}")
    private String offHoursEnd;

    @Scheduled(fixedDelayString = "${app.scheduling.post-session-delay-ms}")
    @Transactional
    public void runPostSessionDetection() {
        List<Session> closedSessions = sessionRepository.findByStatusAndProcessedFalse("CLOSED");
        if (closedSessions.isEmpty()) return;

        log.info("Post-session detection: evaluating {} closed sessions", closedSessions.size());

        for (Session session : closedSessions) {
            detectImpossibleTravel(session);
            detectExcessiveDownloads(session);
            detectSessionDurationOutlier(session);
            detectOffHoursActivity(session);
            detectAccountSharing(session);

            session.setProcessed(true);
            sessionRepository.save(session);
        }
    }

    /**
     * Impossible travel: two sessions for same user, different countries,
     * time difference less than the configured travel threshold.
     *
     * Uses a self-join on sessions (recursive CTE pattern as shown in spec).
     * JSONB context stores the country_code recorded at login time.
     */
    private void detectImpossibleTravel(Session session) {
        String sql = """
                WITH session_pairs AS (
                    SELECT
                        s1.session_id,
                        s1.user_id,
                        s1.start_time_utc,
                        s1.end_time_utc,
                        s1.context->>'country_code'       AS country_1,
                        s2.session_id                     AS next_session_id,
                        s2.start_time_utc                 AS next_start,
                        s2.context->>'country_code'       AS country_2,
                        EXTRACT(EPOCH FROM (s2.start_time_utc - s1.end_time_utc)) / 3600.0 AS gap_hours
                    FROM sessions s1
                    JOIN sessions s2
                        ON s1.user_id = s2.user_id
                        AND s2.start_time_utc > s1.start_time_utc
                        AND s2.start_time_utc - s1.end_time_utc < make_interval(hours => ?)
                    WHERE s1.status = 'CLOSED'
                      AND s2.status = 'CLOSED'
                      AND s1.context->>'country_code' IS DISTINCT FROM s2.context->>'country_code'
                      AND s1.session_id = ?
                )
                SELECT * FROM session_pairs
                """;

        jdbc.query(sql, rs -> {
            String detail = String.format("Country changed %s→%s with only %.1f hours gap",
                    rs.getString("country_1"), rs.getString("country_2"), rs.getDouble("gap_hours"));
            publish(Alert.builder()
                    .alertType("IMPOSSIBLE_TRAVEL")
                    .severity("CRITICAL")
                    .sessionId(session.getSessionId())
                    .userId(session.getUserId())
                    .triggeredAt(Instant.now())
                    .detectionMode("POST_SESSION")
                    .evidence(Map.of(
                            "session_1", rs.getString("session_id"),
                            "session_2", rs.getString("next_session_id"),
                            "country_1", rs.getString("country_1"),
                            "country_2", rs.getString("country_2"),
                            "gap_hours", rs.getDouble("gap_hours"),
                            "details", detail
                    ))
                    .build());
        }, impossibleTravelHours, session.getSessionId());
    }

    /**
     * Excessive downloads: COUNT(ATTACHMENT_DOWNLOAD) within session > configured threshold.
     */
    private void detectExcessiveDownloads(Session session) {
        Integer downloadCount = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM normalized_events
                WHERE session_id = ? AND event_type = 'ATTACHMENT_DOWNLOAD'
                """,
                Integer.class, session.getSessionId());

        if (downloadCount != null && downloadCount > excessiveDownloadThreshold) {
            publish(Alert.builder()
                    .alertType("EXCESSIVE_DOWNLOADS")
                    .severity("HIGH")
                    .sessionId(session.getSessionId())
                    .userId(session.getUserId())
                    .triggeredAt(Instant.now())
                    .detectionMode("POST_SESSION")
                    .evidence(Map.of(
                            "download_count", downloadCount,
                            "threshold", excessiveDownloadThreshold,
                            "details", "Possible data exfiltration: " + downloadCount + " attachment downloads in one session"
                    ))
                    .build());
        }
    }

    /**
     * Session duration outlier: duration_sec > mean + N*stddev of user's historical sessions.
     */
    private void detectSessionDurationOutlier(Session session) {
        if (session.getDurationSec() == null || session.getUserId() == null) return;

        Map<String, Object> stats = jdbc.queryForMap(
                """
                SELECT AVG(duration_sec) AS mean, STDDEV(duration_sec) AS stddev
                FROM sessions
                WHERE user_id = ? AND status = 'CLOSED' AND session_id != ?
                """,
                session.getUserId(), session.getSessionId());

        Double mean = (Double) stats.get("mean");
        Double stddev = (Double) stats.get("stddev");
        if (mean == null || stddev == null || stddev == 0) return;

        double threshold = mean + sessionDurationSigma * stddev;
        if (session.getDurationSec() > threshold) {
            publish(Alert.builder()
                    .alertType("SESSION_DURATION_OUTLIER")
                    .severity("MEDIUM")
                    .sessionId(session.getSessionId())
                    .userId(session.getUserId())
                    .triggeredAt(Instant.now())
                    .detectionMode("POST_SESSION")
                    .evidence(Map.of(
                            "duration_sec", session.getDurationSec(),
                            "mean_sec", Math.round(mean),
                            "stddev_sec", Math.round(stddev),
                            "sigma_threshold", sessionDurationSigma,
                            "details", "Session duration exceeds μ + " + sessionDurationSigma + "σ baseline"
                    ))
                    .build());
        }
    }

    /**
     * Off-hours activity: session start_time outside the user's normal working hours pattern.
     */
    private void detectOffHoursActivity(Session session) {
        if (session.getStartTimeUtc() == null) return;

        Integer offHoursCount = jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM normalized_events ne
                JOIN sessions s ON ne.session_id = s.session_id
                JOIN users u ON s.user_id = u.id
                WHERE ne.session_id = ?
                  AND (
                      TO_CHAR(ne.event_time_utc AT TIME ZONE u.home_timezone, 'HH24:MI') < u.normal_work_start
                   OR TO_CHAR(ne.event_time_utc AT TIME ZONE u.home_timezone, 'HH24:MI') > u.normal_work_end
                  )
                """,
                Integer.class, session.getSessionId());

        if (offHoursCount != null && offHoursCount > 0) {
            publish(Alert.builder()
                    .alertType("OFF_HOURS_ACTIVITY")
                    .severity("MEDIUM")
                    .sessionId(session.getSessionId())
                    .userId(session.getUserId())
                    .triggeredAt(Instant.now())
                    .detectionMode("POST_SESSION")
                    .evidence(Map.of(
                            "off_hours_events", offHoursCount,
                            "session_start_utc", session.getStartTimeUtc().toString(),
                            "details", offHoursCount + " events occurred outside user's normal working hours"
                    ))
                    .build());
        }
    }

    /**
     * Account sharing: same user_id with overlapping session time windows on different devices.
     */
    private void detectAccountSharing(Session session) {
        String sql = """
                SELECT s2.session_id AS concurrent_session,
                       s2.context->>'initial_device_id' AS device_2,
                       s1.context->>'initial_device_id' AS device_1
                FROM sessions s1
                JOIN sessions s2
                    ON s1.user_id = s2.user_id
                    AND s1.session_id != s2.session_id
                    AND s1.start_time_utc < s2.end_time_utc
                    AND s1.end_time_utc > s2.start_time_utc
                WHERE s1.session_id = ?
                  AND s1.context->>'initial_device_id' IS DISTINCT FROM s2.context->>'initial_device_id'
                LIMIT 1
                """;

        jdbc.query(sql, rs -> {
            publish(Alert.builder()
                    .alertType("ACCOUNT_SHARING")
                    .severity("HIGH")
                    .sessionId(session.getSessionId())
                    .userId(session.getUserId())
                    .triggeredAt(Instant.now())
                    .detectionMode("POST_SESSION")
                    .evidence(Map.of(
                            "session_1", session.getSessionId(),
                            "session_2", rs.getString("concurrent_session"),
                            "device_1", String.valueOf(rs.getString("device_1")),
                            "device_2", String.valueOf(rs.getString("device_2")),
                            "details", "Concurrent sessions on different devices for the same user"
                    ))
                    .build());
        }, session.getSessionId());
    }

    private void publish(Alert alert) {
        log.warn("POST-SESSION ALERT [{}] severity={} session={}",
                alert.getAlertType(), alert.getSeverity(), alert.getSessionId());
        alertPublisher.publish(alert);
    }
}
