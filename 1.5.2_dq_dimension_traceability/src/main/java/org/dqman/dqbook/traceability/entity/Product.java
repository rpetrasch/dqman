package org.dqman.dqbook.traceability.entity;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import lombok.Getter;
import lombok.Setter;

/**
 * Product class as an entity to be audited.
 */
@Entity
@Audited // Enables auditing, necessary even when the base class is already annotated
@Getter
@Setter
public class Product extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private Double price;
}

