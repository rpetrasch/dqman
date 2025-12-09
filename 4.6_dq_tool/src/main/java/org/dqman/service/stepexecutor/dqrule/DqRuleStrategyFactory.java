package org.dqman.service.stepexecutor.dqrule;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory for getting the appropriate DQ rule strategy based on rule type
 */
@Component
public class DqRuleStrategyFactory {
    private final Map<String, DqRuleStrategy> strategies;

    /**
     * Spring automatically populates this List with all beans implementing
     * DqRuleStrategy
     */
    @Autowired
    public DqRuleStrategyFactory(List<DqRuleStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        DqRuleStrategy::getSupportedRuleType,
                        strategy -> strategy));
    }

    /**
     * Gets the appropriate strategy for the given rule type
     * 
     * @param ruleType The type of rule (e.g., "SQL", "REGEX")
     * @return The strategy implementation for that rule type
     * @throws IllegalArgumentException if no strategy is found for the rule type
     */
    public DqRuleStrategy getStrategy(String ruleType) {
        DqRuleStrategy strategy = strategies.get(ruleType);
        if (strategy == null) {
            throw new IllegalArgumentException(
                    "No strategy found for rule type: " + ruleType +
                            ". Available types: " + strategies.keySet());
        }
        return strategy;
    }
}
