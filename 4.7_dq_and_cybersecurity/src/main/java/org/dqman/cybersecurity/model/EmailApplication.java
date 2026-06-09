package org.dqman.cybersecurity.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Application or system that emits log events — master data covering the four
 * source systems: email client, Identity Provider, network firewall and MDM/geolocation.
 * Loaded at startup and used during entity resolution to tag events with a
 * canonical application identifier.
 */
@Entity
@Table(name = "applications")
@Data
@NoArgsConstructor
public class EmailApplication {

    /** Surrogate key. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable application name (e.g. {@code Microsoft Outlook}, {@code Azure AD}). */
    private String name;

    /**
     * Category that maps to the Kafka input topic.
     * One of: {@code EMAIL_CLIENT}, {@code IDP}, {@code NETWORK}, {@code GEOLOCATION}.
     */
    private String appType;

    /** Application version — used for log-format detection if schema changes across versions. */
    private String version;
}
