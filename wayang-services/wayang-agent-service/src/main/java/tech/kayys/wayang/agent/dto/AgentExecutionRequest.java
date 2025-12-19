package tech.kayys.wayang.agent.dto;

import java.util.Map;

public record AgentExecutionRequest(
        String tenantId,
        String triggeredBy,
        Map<String, Object> inputs,
        Map<String, Object> context,
        ExecutionMode executionMode,
        long timeoutMs) {
}