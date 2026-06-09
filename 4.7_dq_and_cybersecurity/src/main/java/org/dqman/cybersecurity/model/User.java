package org.dqman.cybersecurity.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Canonical user entity — master data loaded once at startup from the MDM / HR system.
 * Serves as the anchor for entity resolution: incoming log events carry a raw username
 * string which is resolved to this entity via {@link org.dqman.cybersecurity.pipeline.EntityResolutionService}.
 * <p>
 * Working-hours fields ({@code normalWorkStart}, {@code normalWorkEnd}, {@code homeTimezone})
 * drive the off-hours detection rule in the post-session analysis.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
public class User {

    /** Surrogate key — referenced by sessions, normalized_events and alerts. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short login name as it appears in IdP and email logs (e.g. {@code john.doe}). */
    private String username;

    /** Primary email address — used for cross-source correlation. */
    private String email;

    /**
     * ISO 3166-1 alpha-2 country code of the user's home office (e.g. {@code DE}).
     * Used by the new-country and impossible-travel detection rules.
     */
    private String homeCountryCode;

    /** Start of normal working hours in HH:MM format, interpreted in {@code homeTimezone}. */
    private String normalWorkStart;

    /** End of normal working hours in HH:MM format, interpreted in {@code homeTimezone}. */
    private String normalWorkEnd;

    /** IANA timezone identifier (e.g. {@code Europe/Berlin}). */
    private String homeTimezone;
}
