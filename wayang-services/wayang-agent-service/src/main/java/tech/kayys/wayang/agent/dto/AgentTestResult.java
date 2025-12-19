package tech.kayys.wayang.agent.dto;

import java.util.List;
import java.util.Map;

public record AgentTestResult(
        String agentId,
        String status,
        long startTime,
        long endTime,
        Map<String, Object> inputs,
        Map<String, Object> outputs,
        List<String> errors,
        String errorMessage) {
}