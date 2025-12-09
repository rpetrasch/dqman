package org.dqman.service.stepexecutor;

import org.dqman.model.DqFlowStep;
import org.dqman.model.FlowExecution;
import org.dqman.model.StepData;
import org.dqman.service.IntegrationService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Executor for data source steps
 */
@Component
public class DataSourceStepExecutor implements StepExecutor {

    @Autowired
    private IntegrationService integrationService;

    /**
     * Executes a data source step
     * 
     * @param step    to execute
     * @param context of the execution
     * @return result
     */
    @Override
    public StepData execute(DqFlowStep step, FlowExecution context) {
        List<String> metadata = integrationService.getMetadata(step.getIntegration());
        return integrationService.getData(step.getIntegration(), metadata);
    }

    /**
     * Returns the type of step this executor supports (for executor pattern)
     * 
     * @return type of step
     */
    @Override
    public String getSupportedType() {
        return "DATA SOURCE";
    }
}