package org.dqman.dqbook.traceability.entity;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import java.time.LocalDateTime;

/**
 * Base class for entities that need to be audited.
 */
 @MappedSuperclass
 @Audited  // Enables Envers auditing
public abstract class AuditableEntity {

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void setUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
