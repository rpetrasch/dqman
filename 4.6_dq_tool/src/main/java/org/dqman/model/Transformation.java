package org.dqman.model;

import java.util.List;
import java.util.Map;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Transformation extends FlowStepProcessor {
    @ElementCollection
    private List<String> sourceTableFields;
    private String ruleType; // e.g., SQL, REGEX, Python, Java Predicate, TypeScript, JavaScript, Groovy
    private String ruleValue; // The actual SQL or Regex
    @ElementCollection
    private List<String> targetTableFields;

    public Transformation(String name, String description, String type, String status, List<String> sourceTableFields,
            String ruleType, String ruleValue, List<String> targetTableFields) {
        super(name, description, type, status);
        this.sourceTableFields = sourceTableFields;
        this.ruleType = ruleType;
        this.ruleValue = ruleValue;
        this.targetTableFields = targetTableFields;
    }

}