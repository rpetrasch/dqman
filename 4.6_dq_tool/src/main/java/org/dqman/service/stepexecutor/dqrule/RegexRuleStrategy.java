package org.dqman.service.stepexecutor.dqrule;

import org.dqman.model.DqFlowStep;
import org.dqman.model.DqRule;
import org.dqman.model.FlowExecution;
import org.dqman.model.StepData;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Strategy implementation for REGEX-based DQ rules
 */
@Component
@Slf4j
public class RegexRuleStrategy implements DqRuleStrategy {

    @Override
    public StepData execute(DqFlowStep step, DqRule rule, FlowExecution context) {
        log.info("Executing REGEX rule: {} for step ID: {}", rule.getName(), step.getId());

        // TODO: Implement REGEX rule execution logic
        // 1. Get the data from the predecessor step(s)
        StepData previousData = context.getPreviousData(step);
        log.debug("Previous step data: {}", previousData);

        // 2. Get the regex pattern from rule.getRuleValue()
        String regexPattern = rule.getRuleValue();
        log.debug("Regex pattern to apply: {}", regexPattern);

        // 3. Get the source data from context
        // 4. Apply the regex pattern to validate/extract data
        // 5. Return the results

        // For now, return a placeholder result with metadata
        // Using the StepData constructor that takes a key-value pair
        log.warn("REGEX rule execution not yet implemented for rule: {}", rule.getName());

        return new StepData("status", "PENDING_IMPLEMENTATION - REGEX rule: " + rule.getName());
    }

    @Override
    public String getSupportedRuleType() {
        return "REGEX";
    }
}
