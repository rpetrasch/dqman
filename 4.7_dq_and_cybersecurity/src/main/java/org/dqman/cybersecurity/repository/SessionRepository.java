package org.dqman.cybersecurity.repository;

import org.dqman.cybersecurity.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for the {@link Session} entity.
 * Sessions are the first-class correlation unit: every normalized event
 * references a session, and all detection rules operate at the session granularity.
 */
public interface SessionRepository extends JpaRepository<Session, String> {

    /**
     * Returns all closed sessions that have not yet been evaluated by the
     * post-session detection job.  Called by
     * {@link org.dqman.cybersecurity.detection.PostSessionDetectionService}
     * on each scheduled tick.
     *
     * @param status    expected to be {@code "CLOSED"}
     * @return unprocessed sessions in that status
     */
    List<Session> findByStatusAndProcessedFalse(String status);

    /**
     * Returns all sessions for a given user in a particular status.
     * Used internally when building context for concurrent-session checks.
     *
     * @param userId the surrogate key of the {@link org.dqman.cybersecurity.model.User}
     * @param status session lifecycle status ({@code "OPEN"} or {@code "CLOSED"})
     * @return matching sessions, possibly empty
     */
    List<Session> findByUserIdAndStatus(Long userId, String status);
}
