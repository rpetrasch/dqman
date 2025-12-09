package org.dqman.model;

/**
 * Enum representing the status of a step in a DQ flow
 * This is a very simple implementation for states.
 * Better solution: Use the State Pattern
 * Also, the states are not mutually exclusive: COMPLETED + DQ_PASS
 */
public enum StepStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    DQ_PASS, // COMPLETED + DQ_PASS
    DQ_FAIL, // COMPLETED + DQ_FAIL
    FAILED
}
