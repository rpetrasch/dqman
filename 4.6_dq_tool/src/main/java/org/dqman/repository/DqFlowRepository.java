package org.dqman.repository;

import org.dqman.model.DqFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DqFlowRepository extends JpaRepository<DqFlow, Long> {
}
