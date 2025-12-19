package tech.kayys.wayang.workflow.model;

/**
 * Event types.
 */
public enum EventType {
    // Workflow events
    WORKFLOW_CREATED,
    WORKFLOW_STARTED,
    WORKFLOW_COMPLETED,
    WORKFLOW_FAILED,
    WORKFLOW_CANCELLED,
    WORKFLOW_RESUMED,
    STATUS_CHANGED,

    // Node events
    NODE_STARTED,
    NODE_SUCCEEDED,
    NODE_FAILED,
    NODE_BLOCKED,
    NODE_SKIPPED,

    // Error events
    ERROR_OCCURRED,
    ERROR_DECISION,
    RETRY_SCHEDULED,
    SELF_HEAL_ATTEMPT,

    // HITL events
    HUMAN_TASK_CREATED,
    HUMAN_TASK_ASSIGNED,
    HUMAN_TASK_COMPLETED,
    HUMAN_TASK_EXPIRED,

    // Policy events
    POLICY_EVALUATED,
    GUARDRAIL_CHECKED,

    // System events
    CHECKPOINT_CREATED,
    RESOURCE_ALLOCATED
}
