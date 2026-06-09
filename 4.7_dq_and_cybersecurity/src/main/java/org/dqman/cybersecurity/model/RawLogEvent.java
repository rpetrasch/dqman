package org.dqman.cybersecurity.model;

import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * POJO deserialized from Kafka input topics.
 * Each source system (email, IdP, network, geolocation) produces this schema.
 */
@Data
public class RawLogEvent {

    private String eventId;
    private String sourceSystem;    // EMAIL, IDP, NETWORK, GEOLOCATION
    private String sourceFile;
    private Long sourceOffset;

    private Instant eventTimeUtc;   // source clock, normalized to UTC
    private Instant ingestionTime;  // set by producer at Kafka send time

    private String username;
    private String deviceId;
    private String sessionId;
    private String ip;

    private Map<String, Object> payload;   // original log fields, preserved as-is
}
