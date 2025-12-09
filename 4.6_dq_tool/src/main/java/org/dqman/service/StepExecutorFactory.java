package org.dqman.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dqman.service.stepexecutor.StepExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StepExecutorFactory {
    private final Map<String, StepExecutor> executors;

    // Spring automatically populates this List with all beans of type StepExecutor
    @Autowired
    public StepExecutorFactory(List<StepExecutor> executorList) {
        this.executors = executorList.stream().collect(
                Collectors.toMap(StepExecutor::getSupportedType, e -> e));
    }

    public StepExecutor getExecutor(String type) {
        // Now 'type' comes directly from DqFlowStep.getType()
        // Example: "DQ RULE" matches the RuleStepExecutor
        return executors.get(type);
    }
}