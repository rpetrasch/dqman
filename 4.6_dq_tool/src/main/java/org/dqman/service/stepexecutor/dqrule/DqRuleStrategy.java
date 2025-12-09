package org.dqman.service.stepexecutor.dqrule;

import org.dqman.model.DqFlowStep;
import org.dqman.model.DqRule;
import org.dqman.model.FlowExecution;
import org.dqman.model.StepData;

/**
 * Strategy interface for different DQ rule types (Strategy Pattern)
 */
public interface DqRuleStrategy {
    /**
     * Executes the DQ rule logic specific to this rule type
     * 
     * @param step    The current DQ flow step being executed
     * @param rule    The DQ rule to execute
     * @param context The execution context
     * @return StepData with the result of the rule execution
     */
    StepData execute(DqFlowStep step, DqRule rule, FlowExecution context);

    /**
     * Returns the rule type this strategy supports
     * 
     * @return the rule type (e.g., "SQL", "REGEX")
     */
    String getSupportedRuleType();
}
