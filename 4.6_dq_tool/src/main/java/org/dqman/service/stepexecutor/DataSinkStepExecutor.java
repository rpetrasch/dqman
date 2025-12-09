package org.dqman.service.stepexecutor;

import org.dqman.model.DqFlowStep;
import org.dqman.model.FlowExecution;
import org.dqman.model.StepData;
import org.dqman.service.IntegrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataSinkStepExecutor implements StepExecutor {

    @Autowired
    private IntegrationService integrationService;

    @Override
    public StepData execute(DqFlowStep step, FlowExecution context) {
        StepData result = integrationService.writeData(step.getIntegration(),
                context.getPreviousData(step));
        return result;
    }

    @Override
    public String getSupportedType() {
        return "DATA SINK";
    }
}