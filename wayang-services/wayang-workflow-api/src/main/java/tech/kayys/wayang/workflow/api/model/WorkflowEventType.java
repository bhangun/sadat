package tech.kayys.wayang.workflow.api.model;

/**
 * Event types
 */
public enum WorkflowEventType {
    CREATED,
    STATUS_CHANGED,
    NODE_EXECUTED,
    STATE_UPDATED,
    RESUMED,
    CANCELLED
}
