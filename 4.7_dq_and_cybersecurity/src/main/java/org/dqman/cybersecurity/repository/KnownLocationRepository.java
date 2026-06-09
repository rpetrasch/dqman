package org.dqman.cybersecurity.repository;

import org.dqman.cybersecurity.model.KnownLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link KnownLocation} master data.
 * Used during entity resolution to map an event IP address to a canonical
 * corporate location (office, home-office range, VPN exit node).
 */
public interface KnownLocationRepository extends JpaRepository<KnownLocation, Long> {

    /**
     * Matches an event IP address against the stored CIDR prefixes using a
     * simple prefix-match heuristic.  A full CIDR implementation would use
     * PostgreSQL's {@code <<} inet operator; this JPQL approximation is
     * sufficient for the fixed prefix ranges in the seed data.
     *
     * @param ip the dotted-decimal IPv4 address from the log event
     * @return the first matching known location, or empty for external / unknown IPs
     */
    @Query(value = "SELECT * FROM known_locations WHERE CAST(?1 AS inet) << ip_cidr::inet ORDER BY id LIMIT 1", nativeQuery = true)
    Optional<KnownLocation> findByIpPrefix(String ip);
}
