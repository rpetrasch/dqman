package org.dqman.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.FetchType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a step in a data quality flow.
 */
@Entity
@Data
@NoArgsConstructor
public class DqFlowStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String type; // DATA SOURCE, DQ RULE, TRANSFORMATION, DECISION, DATA SINK

    @ManyToOne
    @JoinColumn(name = "flow_id")
    @JsonBackReference
    private DqFlow flow;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "flow_step_successor", joinColumns = @JoinColumn(name = "flow_step_id"), inverseJoinColumns = @JoinColumn(name = "successor_id"))
    @JsonIgnore // Ignore successors to prevent circular reference - use getSuccessorIds()
                // instead
    private List<DqFlowStep> successors = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "flow_step_predecessor", joinColumns = @JoinColumn(name = "flow_step_id"), inverseJoinColumns = @JoinColumn(name = "predecessor_id"))
    @JsonIgnore // Ignore predecessors to prevent circular reference
    private List<DqFlowStep> predecessors = new ArrayList<>();

    // ToDo The following references are mutual exclusive. Better solution would be
    // to use a single reference to a base class. This is a Violation of the
    // open-close principle!

    // Reference to integration (for DATA SOURCE or DATA SINK steps)
    @ManyToOne
    @JoinColumn(name = "integration_id")
    private DataIntegration integration;

    // Reference to DQ rule (for DQ RULE steps)
    @ManyToOne
    @JoinColumn(name = "rule_id")
    private DqRule rule;

    // Reference to transformation (for TRANSFORMATION steps)
    @ManyToOne
    @JoinColumn(name = "transformation_id")
    private Transformation transformation;

    // Manual override for initial/final step designation
    private Boolean isInitial = false;
    private Boolean isFinal = false;

    // Graph layout positions
    private Integer posX;
    private Integer posY;

    public void addSuccessor(DqFlowStep successor) {
        if (successor != null && !this.successors.contains(successor)) {
            this.successors.add(successor);
            // Maintain bidirectional relationship
            if (!successor.getPredecessors().contains(this)) {
                successor.getPredecessors().add(this);
            }
        }
    }

    public void addPredecessor(DqFlowStep predecessor) {
        if (predecessor != null && !this.predecessors.contains(predecessor)) {
            this.predecessors.add(predecessor);
            // Maintain bidirectional relationship
            if (!predecessor.getSuccessors().contains(this)) {
                predecessor.getSuccessors().add(this);
            }
        }
    }

    // Helper methods for JSON serialization of IDs
    public List<Long> getSuccessorIds() {
        return new ArrayList<>(successors.stream()
                .map(DqFlowStep::getId)
                .filter(id -> id != null)
                .toList());
    }

    public List<Long> getPredecessorIds() {
        return new ArrayList<>(predecessors.stream()
                .map(DqFlowStep::getId)
                .filter(id -> id != null)
                .toList());
    }

    // Helper methods for JSON serialization of integration/rule/transformation IDs
    public Long getIntegrationId() {
        return integration != null ? integration.getId() : null;
    }

    public Long getRuleId() {
        return rule != null ? rule.getId() : null;
    }

    public Long getTransformationId() {
        return transformation != null ? transformation.getId() : null;
    }

}
