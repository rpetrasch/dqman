package org.dqman.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    // Reference to integration (for DATA SOURCE or DATA SINK steps)
    @ManyToOne
    @JoinColumn(name = "integration_id")
    private DqIntegration integration;

    // Reference to DQ rule (for DQ RULE steps)
    @ManyToOne
    @JoinColumn(name = "rule_id")
    private DqRule rule;
}
