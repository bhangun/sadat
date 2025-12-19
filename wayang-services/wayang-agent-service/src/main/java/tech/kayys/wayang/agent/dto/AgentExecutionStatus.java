package tech.kayys.wayang.agent.dto;

public record AgentExecutionStatus(
        String executionId,
        AgentStatus status,
        String currentStep,
        double progress) {
}