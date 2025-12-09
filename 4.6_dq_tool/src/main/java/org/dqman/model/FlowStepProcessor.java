package org.dqman.model;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.NoArgsConstructor;

@MappedSuperclass
@Data
@NoArgsConstructor
public abstract class FlowStepProcessor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String type; // DATA SOURCE, DQ RULE, TRANSFORMATION, DECISION, DATA SINK
    private String status; // CREATED, VALIDATED, PRODUCTION, ARCHIVED
    private ZonedDateTime createdDate;
    private ZonedDateTime modifiedDate;

    public FlowStepProcessor(String name, String description, String type, String status) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.status = status;
    }

}
