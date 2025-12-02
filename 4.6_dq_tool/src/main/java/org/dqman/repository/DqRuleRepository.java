package org.dqman.repository;

import org.dqman.model.DqRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DqRuleRepository extends JpaRepository<DqRule, Long> {
}
