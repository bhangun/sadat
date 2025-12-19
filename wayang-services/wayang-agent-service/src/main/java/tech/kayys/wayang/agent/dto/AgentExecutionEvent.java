package tech.kayys.wayang.agent.dto;

public record AgentExecutionEvent(
        String eventType,
        String agentId,
        String executionId,
        Object data,
        java.time.LocalDateTime timestamp) {
}