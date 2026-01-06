package tech.kayys.wayang.agent.dto;

import java.util.Map;
import java.util.Set;

/**
 * Plan Step
 */
public record PlanStep(
    String stepId,
    String description,
    String assignedAgentType,
    Map<String, Object> stepContext,
    Set<String> dependencies,
    StepStatus status,
    AgentExecutionResult result
) {
    
    public boolean isCompleted() {
        return status == StepStatus.COMPLETED;
    }
    
    public boolean canExecute(Set<String> completedSteps) {
        return dependencies.stream().allMatch(completedSteps::contains);
    }
}