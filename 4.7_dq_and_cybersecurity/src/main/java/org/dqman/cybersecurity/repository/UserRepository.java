package org.dqman.cybersecurity.repository;

import org.dqman.cybersecurity.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link User} master data.
 * Results are served through the Caffeine cache in
 * {@link org.dqman.cybersecurity.pipeline.EntityResolutionService}
 * so that each Kafka event does not incur a DB round-trip.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Looks up a user by their login name as it appears in IdP and email log events.
     *
     * @param username the short login name (e.g. {@code john.doe})
     * @return the matching user, or empty if the username is not in the master data
     */
    Optional<User> findByUsername(String username);
}
