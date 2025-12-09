package org.dqman.model;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents the execution of a DQ flow
 */
@Entity
@NoArgsConstructor
public class FlowExecution {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    private DqFlow flow; // The DQ flow to execute
    @Getter
    private Long flowId;

    @Transient
    @Getter
    private DataSource dataSource; // SQL data source (database connection) for SQL execution

    @Transient
    private java.util.concurrent.CountDownLatch completionLatch; // Tracks completion of all steps

    @Transient
    private final Map<Long, StepStatus> stepStatuses = new ConcurrentHashMap<>(); // Tracks the status of each step
    @Transient
    private final Map<Long, StepData> stepDataMap = new ConcurrentHashMap<>(); // Tracks the data for each step

    // Tracks how many inputs have arrived for merge nodes
    @Transient
    private final Map<Long, AtomicInteger> arrivalCounters = new ConcurrentHashMap<>();

    /**
     * Creates a new FlowExecution instance
     * 
     * @param flow       The DQ flow to execute
     * @param dataSource The DataSource for SQL execution
     */
    public FlowExecution(DqFlow flow, DataSource dataSource) {
        this.flow = flow;
        this.flowId = flow.getId();
        this.dataSource = dataSource;
    }

    /**
     * Sets the completion latch for tracking flow completion
     * 
     * @param latch The CountDownLatch to use
     */
    public void setCompletionLatch(java.util.concurrent.CountDownLatch latch) {
        this.completionLatch = latch;
    }

    /**
     * Counts down the completion latch when a step completes
     */
    public void countDownStep() {
        if (completionLatch != null) {
            completionLatch.countDown();
        }
    }

    /**
     * Saves the data for a step in the execution context
     * 
     * @param stepId The ID of the step
     * @param data   The data to save
     */
    public void saveStepData(Long stepId, StepData data) {
        stepDataMap.put(stepId, data);
    }

    /**
     * Retrieves the data for a step from the execution context
     * 
     * @param stepId The ID of the step
     * @return The data for the step
     */
    public StepData getPreviousStepData(Long stepId) {
        return stepDataMap.get(stepId);
    }

    /**
     * Increments the arrival counter for a step and returns the new value
     * 
     * @param stepId The ID of the step
     * @return The new arrival count
     */
    public int incrementAndGetArrivals(Long stepId) {
        return arrivalCounters.computeIfAbsent(stepId, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    /**
     * Fetches data from all predecessors and returns it as a Map
     * keyed by the predecessor's name or ID.
     */
    public StepData getPreviousData(DqFlowStep currentStep) {
        Map<Long, StepData> inputs = new HashMap<>();

        for (DqFlowStep pred : currentStep.getPredecessors()) {
            StepData data = stepDataMap.get(pred.getId());
            if (data != null) {
                inputs.put(pred.getId(), data);
            }
        }
        // ToDo implement proper logic for multiple predecessors
        return inputs.values().stream().findFirst().orElse(null);
    }

    /**
     * Updates the status of a step in the execution context
     * 
     * @param id     The ID of the step
     * @param status The new status
     */
    public void updateStatus(Long id, StepStatus status) {
        stepStatuses.put(id, status);
    }

    /**
     * Saves an error for a step in the execution context
     * 
     * @param id     The ID of the step
     * @param string The error message
     */
    public void saveStepError(Long id, String string) {
        stepDataMap.put(id, new StepData("Error", string));

    }

    /**
     * Gets all step results from the execution
     * 
     * @return Map of step IDs to their result data
     */
    public Map<Long, StepData> getAllStepResults() {
        return new HashMap<>(stepDataMap);
    }

    /**
     * Gets all step statuses from the execution
     * 
     * @return Map of step IDs to their statuses
     */
    public Map<Long, StepStatus> getAllStepStatuses() {
        return new HashMap<>(stepStatuses);
    }
}
