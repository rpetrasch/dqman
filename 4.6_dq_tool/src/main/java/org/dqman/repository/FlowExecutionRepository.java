package org.dqman.repository;

import org.dqman.model.FlowExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for FlowExecution
 */
@Repository
public interface FlowExecutionRepository extends JpaRepository<FlowExecution, Long> {
}
