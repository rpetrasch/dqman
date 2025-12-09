package org.dqman.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a data quality flow.
 */
@Entity
@Data
@NoArgsConstructor
public class DqFlow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String status; // CREATED, VALIDATED, PRODUCTION, ARCHIVED
    private ZonedDateTime createdDate;
    private ZonedDateTime modifiedDate;

    @OneToMany(mappedBy = "flow", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<DqFlowStep> steps = new ArrayList<>();

    public void addStep(DqFlowStep step) {
        steps.add(step);
        step.setFlow(this);
    }

    public void removeStep(DqFlowStep step) {
        steps.remove(step);
        step.setFlow(null);
    }
}
