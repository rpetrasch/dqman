package org.dqman.repository;

import org.dqman.model.DqProject;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DqProjectRepository extends JpaRepository<DqProject, Long> {
}
