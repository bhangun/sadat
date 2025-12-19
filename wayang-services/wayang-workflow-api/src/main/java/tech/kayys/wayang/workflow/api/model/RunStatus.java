package tech.kayys.wayang.workflow.api.model;

/**
 * Run status enum.
 */
public enum RunStatus {
    PAUSED,
    AWAITING_HITL,
    COMPLETED,
    BLOCKED,
    PENDING, // Created, not started
    RUNNING, // Actively executing
    SUSPENDED, // Paused (HITL, wait state)
    SUCCEEDED, // Completed successfully
    FAILED, // Failed permanently
    CANCELLED, // User cancelled
    TIMED_OUT, // Exceeded timeout
    COMPENSATING // Running compensation
}
