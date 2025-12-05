package org.dqman.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class DqRule extends FlowComponent {
    private String ruleType; // e.g., SQL, REGEX
    private String ruleValue; // The actual SQL or Regex
    private String targetTable;
    private String targetColumn;
}
