package org.dqman.repository;

import org.dqman.model.DqIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DqIntegrationRepository extends JpaRepository<DqIntegration, Long> {
}
