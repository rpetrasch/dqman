package org.dqman.cybersecurity.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "sessions")
@Data
@NoArgsConstructor
public class Session {

    @Id
    private String sessionId;

    private Long userId;
    private Instant startTimeUtc;
    private Instant endTimeUtc;
    private Long durationSec;
    private String status;          // OPEN, CLOSED
    private Double riskScore;
    private Boolean processed;

    /**
     * JSONB bag: initial IP, device, country code at login time.
     * Preserved for post-session CTE queries and forensic drill-down.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> context;
}
