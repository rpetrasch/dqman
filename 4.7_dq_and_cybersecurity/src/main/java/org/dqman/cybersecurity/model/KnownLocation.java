package org.dqman.cybersecurity.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A named, trusted network location — master data covering corporate offices,
 * home-office IP ranges and VPN exit nodes.
 * <p>
 * During entity resolution each incoming event IP is matched against the
 * {@code ipCidr} prefix of all known locations. An IP that does not match any
 * known location is treated as an unknown / external network and adds to the
 * session risk score.  The resolved {@code countryCode} drives both the
 * new-country (streaming) and impossible-travel (post-session) detection rules.
 */
@Entity
@Table(name = "known_locations")
@Data
@NoArgsConstructor
public class KnownLocation {

    /** Surrogate key — stored as {@code location_id} in normalized_events. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable label (e.g. {@code Berlin HQ}, {@code John Home Office}). */
    private String name;

    /**
     * Location category.
     * One of: {@code OFFICE}, {@code HOME}, {@code VPN}.
     */
    private String locationType;

    /**
     * ISO 3166-1 alpha-2 country code (e.g. {@code DE}, {@code TH}).
     * Stored in the session context JSONB at login time for CTE-based travel detection.
     */
    private String countryCode;

    /**
     * CIDR notation of the IP range for this location (e.g. {@code 10.0.0.0/8}).
     * Lookup is performed as a prefix match against the event IP.
     */
    private String ipCidr;

    /** WGS-84 latitude — informational, used for geographic visualisation. */
    private Double latitude;

    /** WGS-84 longitude — informational, used for geographic visualisation. */
    private Double longitude;
}
