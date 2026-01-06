package tech.kayys.wayang.agent.dto;

/**
 * Orchestration Event Type
 */
public enum OrchestrationEventType {
    PLAN_CREATED,
    STEP_STARTED,
    STEP_COMPLETED,
    STEP_FAILED,
    AGENT_SELECTED,
    COORDINATION_INITIATED,
    EVALUATION_COMPLETED,
    REPLANNING_TRIGGERED,
    ORCHESTRATION_COMPLETED
}
