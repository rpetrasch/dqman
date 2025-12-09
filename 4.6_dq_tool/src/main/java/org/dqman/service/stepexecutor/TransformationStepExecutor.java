package org.dqman.service.stepexecutor;

import org.dqman.model.DqFlowStep;
import org.dqman.model.FlowExecution;
import org.dqman.model.StepData;
import org.springframework.stereotype.Component;

/**
 * Executor for transformation steps
 */
@Component
public class TransformationStepExecutor implements StepExecutor {

    /**
     * Executes a transformation step
     * 
     * @param step    to execute
     * @param context of the execution
     * @return result
     */
    @Override
    public StepData execute(DqFlowStep step, FlowExecution context) {
        throw new UnsupportedOperationException("Unimplemented method 'perform'");
    }

    /**
     * Returns the type of step this executor supports (for executor pattern)
     * 
     * @return type of step
     */
    @Override
    public String getSupportedType() {
        return "TRANSFORMATION";
    }
}