package org.dqman.service.stepexecutor;

import java.util.List;
import java.util.Map;

import org.dqman.model.DqFlowStep;
import org.dqman.model.FlowExecution;
import org.dqman.model.StepData;

/**
 * Interface for step executors (executor pattern)
 */
public interface StepExecutor {
    /**
     * @param step    The step definition
     * @param context The execution state object to store results
     * @return StepData with the result of the step execution
     */
    StepData execute(DqFlowStep step, FlowExecution context);

    /**
     * Explicitly define which step type this executor handles
     * 
     * @return the type of step this executor handles
     */
    String getSupportedType();
}
