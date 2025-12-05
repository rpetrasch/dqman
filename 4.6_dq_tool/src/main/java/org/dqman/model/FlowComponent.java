package org.dqman.model;

import java.util.List;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class FlowComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    @OneToMany
    private List<FlowComponent> inputComponents;
    @ElementCollection
    private List<String> inputValues;
    @ManyToOne
    private FlowComponent processComponent;
    @OneToMany
    private List<FlowComponent> outputComponents;
    @ElementCollection
    private List<String> outputValues;
    private List<String> outputLogic;
}
