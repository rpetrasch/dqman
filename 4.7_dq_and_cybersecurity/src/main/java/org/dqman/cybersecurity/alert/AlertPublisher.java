package org.dqman.cybersecurity.alert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dqman.cybersecurity.model.Alert;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

/**
 * Publishes alerts to the security.alerts Kafka topic (consumed by Kafbat UI
 * and the DB alert writer) and persists them to the alerts table for historical
 * querying and correlation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.alerts}")
    private String alertTopic;

    /**
     * Publishes an alert to the Kafka {@code security.alerts} topic and writes it to the
     * {@code alerts} DB table.  Both operations are best-effort: a serialization failure
     * is logged but does not throw, so it cannot poison the main event pipeline transaction.
     *
     * @param alert the alert to publish; {@code alertId} must be non-null and unique
     */
    public void publish(Alert alert) {
        try {
            String json = objectMapper.writeValueAsString(alert);

            // Publish to Kafka alert topic — consumed by Kafbat UI and downstream SIEM
            kafkaTemplate.send(alertTopic, alert.getAlertId(), json);

            // Persist to DB for historical querying and correlation
            persistAlert(alert, json);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize alert {}: {}", alert.getAlertId(), e.getMessage());
        }
    }

    private void persistAlert(Alert alert, String evidenceJson) {
        try {
            String evidenceStr = objectMapper.writeValueAsString(alert.getEvidence());
            jdbc.update(
                    """
                    INSERT INTO alerts (alert_id, alert_type, severity, session_id, user_id,
                                       triggered_at, detection_mode, evidence)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                    ON CONFLICT (alert_id) DO NOTHING
                    """,
                    alert.getAlertId(),
                    alert.getAlertType(),
                    alert.getSeverity(),
                    alert.getSessionId(),
                    alert.getUserId(),
                    alert.getTriggeredAt() != null ? Timestamp.from(alert.getTriggeredAt()) : null,
                    alert.getDetectionMode(),
                    evidenceStr
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to persist alert evidence for {}", alert.getAlertId());
        }
    }
}
