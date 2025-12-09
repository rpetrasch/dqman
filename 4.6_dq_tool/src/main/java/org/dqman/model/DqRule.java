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
public class DqRule extends FlowStepProcessor {
    private String ruleType; // e.g., SQL, REGEX
    private String ruleValue; // The actual SQL or Regex
    @ElementCollection
    private List<String> sourceTableFieldList;
    @ElementCollection
    private List<String> resultList;

    public DqRule(String name, String description, String type, String status, String ruleType, String ruleValue,
            List<String> sourceTableFieldList, List<String> resultList) {
        super(name, description, type, status);
        this.ruleType = ruleType; // e.g., SQL, REGEX. Not an enum to allow for future extensions via plugins
        this.ruleValue = ruleValue;
        this.sourceTableFieldList = sourceTableFieldList;
        this.resultList = resultList;
    }

}
