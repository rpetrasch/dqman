package org.dqman.cybersecurity.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Alert event published to security.alerts Kafka topic and persisted to the alerts table.
 * The evidence field closes the full drill-down chain:
 * Alert → Session → Normalized Events → Raw Events → Original Log Line
 */
@Data
@Builder
public class Alert {

    @Builder.Default
    private String alertId = UUID.randomUUID().toString();

    private String alertType;
    private String severity;        // LOW, MEDIUM, HIGH, CRITICAL
    private String sessionId;
    private Long userId;
    private Instant triggeredAt;
    private String detectionMode;   // STREAMING, POST_SESSION

    /**
     * Full forensic evidence chain:
     * - eventIds: list of normalized_event.event_id records that triggered the alert
     * - rawEventIds: corresponding raw_event.event_id records (immutable originals)
     * - details: human-readable context (e.g. "country Berlin→Bangkok, 90 min gap")
     */
    private Map<String, Object> evidence;
}
