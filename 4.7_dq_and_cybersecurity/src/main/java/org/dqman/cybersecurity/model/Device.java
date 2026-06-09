package org.dqman.cybersecurity.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Known corporate device — master data imported from the MDM system.
 * Device trust level is used by the new-device detection rule:
 * an event arriving from a {@code deviceId} not present in this table for the
 * given user triggers a {@code NEW_DEVICE} streaming alert.
 */
@Entity
@Table(name = "devices")
@Data
@NoArgsConstructor
public class Device {

    /** Surrogate key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique hardware or MDM identifier as it appears in log events
     * (e.g. {@code CORP-LAPTOP-001}).
     */
    private String deviceId;

    /** Network hostname, informational only. */
    private String hostname;

    /**
     * MDM-assigned trust classification.
     * One of: {@code CORPORATE}, {@code PERSONAL}, {@code UNKNOWN}.
     * Only {@code CORPORATE} devices pass the new-device rule silently.
     */
    private String trustLevel;

    /**
     * User who owns this device according to MDM.
     * {@code null} for unregistered / unknown devices.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;
}
