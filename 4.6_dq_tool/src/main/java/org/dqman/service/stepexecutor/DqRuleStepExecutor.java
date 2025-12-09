package org.dqman.service.stepexecutor;

import org.dqman.model.DqFlowStep;
import org.dqman.model.DqRule;
import org.dqman.model.FlowExecution;
import org.dqman.model.StepData;
import org.dqman.service.stepexecutor.dqrule.DqRuleStrategy;
import org.dqman.service.stepexecutor.dqrule.DqRuleStrategyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Executor for DQ rule steps
 * Uses Strategy Pattern to delegate to specific rule type implementations
 */
@Component
@Slf4j
public class DqRuleStepExecutor implements StepExecutor {

    @Autowired
    private DqRuleStrategyFactory strategyFactory;

    /**
     * Executes a DQ rule step by delegating to the appropriate strategy
     * based on the rule type (SQL, REGEX, etc.)
     * 
     * @param step    to execute
     * @param context of the execution
     * @return result
     */
    @Override
    public StepData execute(DqFlowStep step, FlowExecution context) {
        log.info("Executing DQ Rule step: {}", step.getName());

        // Get the rule from the step
        DqRule rule = step.getRule();
        if (rule == null) {
            log.error("No DQ rule configured for step: {}", step.getName());
            throw new IllegalStateException("No DQ rule configured for step: " + step.getName());
        }

        // Get the rule type
        String ruleType = rule.getRuleType();
        if (ruleType == null || ruleType.isEmpty()) {
            log.error("Rule type not specified for rule: {}", rule.getName());
            throw new IllegalStateException("Rule type not specified for rule: " + rule.getName());
        }

        log.debug("Rule type: {}, Rule value: {}", ruleType, rule.getRuleValue());

        // Get the appropriate strategy and execute
        DqRuleStrategy strategy = strategyFactory.getStrategy(ruleType);
        return strategy.execute(step, rule, context);
    }

    /**
     * Returns the type of step this executor supports (for executor pattern)
     * 
     * @return type of step
     */
    @Override
    public String getSupportedType() {
        return "DQ RULE";
    }
}