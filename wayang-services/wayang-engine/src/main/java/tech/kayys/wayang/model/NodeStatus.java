package tech.kayys.wayang.model;

/**
 * Represents the execution status of an individual node.
 * Designed to support retry logic and error handling patterns.
 */
public enum NodeStatus {
    /**
     * Node is waiting for dependencies
     */
    PENDING,

    /**
     * Node is currently executing
     */
    RUNNING,

    /**
     * Node completed successfully
     */
    SUCCESS,

    /**
     * Node failed with error
     */
    FAILED,

    /**
     * Node encountered an error (different from FAILED - may be transient)
     */
    ERROR,

    /**
     * Node is scheduled for retry
     */
    RETRYING,

    /**
     * Node was skipped (conditional logic)
     */
    SKIPPED,

    /**
     * Node execution cancelled
     */
    CANCELLED,

    /**
     * Waiting for human decision
     */
    AWAITING_HITL;

    public boolean isTerminal() {
        return this == SUCCESS || this == SKIPPED || this == CANCELLED;
    }

    public boolean isFailed() {
        return this == FAILED || this == ERROR;
    }

    public boolean isActive() {
        return this == RUNNING || this == RETRYING;
    }
}