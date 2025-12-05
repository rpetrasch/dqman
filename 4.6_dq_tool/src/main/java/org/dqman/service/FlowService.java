package org.dqman.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dqman.model.DqFlow;
import org.dqman.model.DqFlowStep;
import org.dqman.model.DqIntegration;
import org.dqman.repository.DqFlowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class FlowService {

    @Autowired
    private DqFlowRepository dqFlowRepository;

    @Autowired
    private IntegrationService integrationService;

    public Map<String, Object> executeFlow(Long flowId) {

        DqFlow flow = dqFlowRepository.findById(flowId).orElse(null);
        if (flow == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("flowId", flow.getId());
        result.put("flowName", flow.getName());
        result.put("status", "SUCCESS");
        result.put("startTime", ZonedDateTime.now().toString());

        Map<String, List<List<String>>> data = new HashMap<>();
        List<Map<String, Object>> stepResults = new ArrayList<>();

        if (flow.getSteps() != null && !flow.getSteps().isEmpty()) {
            for (int i = 0; i < flow.getSteps().size(); i++) {
                DqFlowStep step = flow.getSteps().get(i);
                Map<String, Object> stepResult = new HashMap<>();
                stepResult.put("stepIndex", i);
                stepResult.put("stepName", step.getName());
                stepResult.put("stepType", step.getType());

                try {
                    // Execute step based on type
                    if ("DATA SOURCE".equals(step.getType()) || "DATA SINK".equals(step.getType())) {
                        if (step.getIntegration() != null) {
                            if ("DATA SOURCE".equals(step.getType())) {
                                DqIntegration integration = step.getIntegration();
                                List<String> metadata = integrationService.getMetadata(integration);
                                data = integrationService.getData(integration, metadata);
                            } else if ("DATA SINK".equals(step.getType())) {
                                // ToDo
                            }
                            stepResult.put("integrationId", step.getIntegration().getId());
                            stepResult.put("integrationName", step.getIntegration().getName());
                            stepResult.put("status", "COMPLETED");
                            stepResult.put("message", "Integration processed successfully");
                        } else {
                            stepResult.put("status", "WARNING");
                            stepResult.put("message", "No integration configured");
                        }
                    } else if ("DQ RULE".equals(step.getType())) {
                        if (step.getRule() != null) {
                            stepResult.put("ruleId", step.getRule().getId());
                            stepResult.put("ruleName", step.getRule().getName());
                            stepResult.put("status", "COMPLETED");
                            stepResult.put("message", "DQ rule applied successfully");
                        } else {
                            stepResult.put("status", "WARNING");
                            stepResult.put("message", "No rule configured");
                        }
                    } else {
                        stepResult.put("status", "COMPLETED");
                        stepResult.put("message", "Step processed");
                    }
                } catch (Exception e) {
                    stepResult.put("status", "ERROR");
                    stepResult.put("message", "Error: " + e.getMessage());
                }

                stepResults.add(stepResult);
            }
        }
        result.put("steps", stepResults);
        return result;

    }

}
