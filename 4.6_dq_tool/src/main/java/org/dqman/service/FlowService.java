package org.dqman.service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

import org.dqman.model.DqFlow;
import org.dqman.model.DqFlowStep;
import org.dqman.model.FlowExecution;
import org.dqman.model.StepData;
import org.dqman.model.StepStatus;
import org.dqman.model.DataIntegration;
import org.dqman.repository.DqFlowRepository;
import org.dqman.repository.FlowExecutionRepository;
import org.dqman.service.stepexecutor.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Service class for flow execution
 */
@Service
@Slf4j
public class FlowService {

    @Autowired
    private DqFlowRepository dqFlowRepository;

    @Autowired
    private IntegrationService integrationService;

    @Autowired
    StepExecutorFactory stepExecutorFactory;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private FlowExecutionRepository flowExecutionRepository;

    /**
     * Executes a flow (old implementation)
     * 
     * @param flowId
     * @return execution result
     */
    public Map<String, Object> executeFlowDEPRECATED(Long flowId) {

        DqFlow flow = dqFlowRepository.findById(flowId).orElse(null);
        if (flow == null) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("flowId", flow.getId());
        result.put("flowName", flow.getName());
        result.put("status", "SUCCESS");
        result.put("startTime", ZonedDateTime.now().toString());

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
                                DataIntegration integration = step.getIntegration();
                                List<String> metadata = integrationService.getMetadata(integration);
                                StepData data = integrationService.getData(integration, metadata);
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

    /**
     * Starts the execution of a flow using a thread-safe parallel algorithm
     * This approach uses a recursive execution pattern where each branch spawns its
     * own thread.
     * 
     * @param flowId to execute
     */
    public Map<String, Object> executeFlow(Long flowId) {
        Map<String, Object> result = new HashMap<>();
        DqFlow flow = dqFlowRepository.findById(flowId).orElse(null);
        if (flow == null) {
            throw new RuntimeException("Flow not found");
        }
        FlowExecution execution = new FlowExecution(flow, dataSource);
        flowExecutionRepository.save(execution);

        ExecutorService executor = Executors.newCachedThreadPool();
        ZonedDateTime startTime = ZonedDateTime.now();

        // Count total number of steps to execute
        int totalSteps = flow.getSteps().size();
        java.util.concurrent.CountDownLatch completionLatch = new java.util.concurrent.CountDownLatch(totalSteps);

        // Store the latch in the execution context so steps can count down when
        // complete
        execution.setCompletionLatch(completionLatch);

        // Trigger all initial steps in parallel
        flow.getSteps().stream()
                .filter(DqFlowStep::getIsInitial)
                .forEach(step -> executeAsync(step, execution, executor));

        // Wait for all tasks to complete (with timeout)
        try {
            // Wait up to 5 minutes for all tasks to complete
            if (!completionLatch.await(5, java.util.concurrent.TimeUnit.MINUTES)) {
                log.warn("Flow execution timed out after 5 minutes");
            }
        } catch (InterruptedException e) {
            log.error("Flow execution interrupted", e);
            Thread.currentThread().interrupt();
        } finally {
            // Now it's safe to shutdown the executor
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        ZonedDateTime endTime = ZonedDateTime.now();

        // Build result with step execution data
        result.put("flowId", flow.getId());
        result.put("flowName", flow.getName());
        result.put("status", "SUCCESS");
        result.put("startTime", startTime.toString());
        result.put("endTime", endTime.toString());

        // Add step results
        List<Map<String, Object>> stepResults = new ArrayList<>();
        Map<Long, StepData> allStepData = execution.getAllStepResults();
        Map<Long, StepStatus> allStepStatuses = execution.getAllStepStatuses();

        for (DqFlowStep step : flow.getSteps()) {
            Map<String, Object> stepResult = new HashMap<>();
            stepResult.put("stepId", step.getId());
            stepResult.put("stepName", step.getName());
            stepResult.put("stepType", step.getType());

            StepData stepData = allStepData.get(step.getId());
            if (stepData != null) {
                stepResult.put("data", stepData.data());
            }

            StepStatus stepStatus = allStepStatuses.get(step.getId());
            if (stepStatus != null) {
                stepResult.put("status", stepStatus.toString());
            } else {
                stepResult.put("status", StepStatus.COMPLETED.toString());
            }

            stepResults.add(stepResult);
        }

        result.put("steps", stepResults);
        result.put("totalSteps", stepResults.size());

        return result;
    }

    /**
     * Executes a step asynchronously
     * 
     * @param step     to execute
     * @param context  of the execution
     * @param executor service to use
     */
    private void executeAsync(DqFlowStep step, FlowExecution context, ExecutorService executor) {
        executor.submit(() -> {
            try {
                // Run the specific logic (Rule, Transformation, etc.)
                performWork(step, context);

                // Process Successors
                for (DqFlowStep successor : step.getSuccessors()) {
                    handleSuccessor(successor, context, executor);
                }
            } catch (Exception e) {
                handleError(step, context, e);
            } finally {
                // Count down the latch to signal this step is complete
                context.countDownStep();
            }
        });
    }

    /**
     * Performs the specific logic for a step
     * 
     * @param step    to execute
     * @param context of the execution
     */
    public void performWork(DqFlowStep step, FlowExecution context) {
        // Mark step as running
        context.updateStatus(step.getId(), StepStatus.RUNNING);

        StepData resultData;

        StepExecutor executor = stepExecutorFactory.getExecutor(step.getType());
        resultData = executor.execute(step, context);
        context.saveStepData(step.getId(), resultData);

        // Mark step as completed
        context.updateStatus(step.getId(), StepStatus.COMPLETED);
    }

    /**
     * Handles the execution of a successor step
     * 
     * @param next     successor step to execute
     * @param context  of the execution
     * @param executor service to use
     */
    private void handleSuccessor(DqFlowStep next, FlowExecution context, ExecutorService executor) {
        int arrivals = context.incrementAndGetArrivals(next.getId());
        int required = next.getPredecessors().size();

        // MERGE LOGIC:
        // Only proceed if it's the last thread to arrive at the junction
        if (arrivals >= required) {
            executeAsync(next, context, executor);
        }
    }

    private void handleError(DqFlowStep step, FlowExecution context, Exception e) {
        // 1. Log the full stack trace for debugging
        log.error("Execution failed at step {}: {}", step.getName(), e.getMessage(), e);

        // 2. Update the status in the central execution state
        context.updateStatus(step.getId(), StepStatus.FAILED);

        // 3. (Optional) Save the error message in the data map for later inspection
        context.saveStepError(step.getId(), "Error: " + e.getMessage());

        // 4. Trigger logic based on graph requirements
        // handleBranchFailure(step, context); // ToDo: Implement
    }
}
