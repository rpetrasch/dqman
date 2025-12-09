package org.dqman.repository;

import org.dqman.model.DataIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataIntegrationRepository extends JpaRepository<DataIntegration, Long> {
}
