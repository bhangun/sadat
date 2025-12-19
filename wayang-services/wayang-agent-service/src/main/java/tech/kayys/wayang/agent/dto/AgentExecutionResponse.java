package tech.kayys.wayang.agent.dto;

import java.util.List;
import java.util.Map;

public record AgentExecutionResponse(
        String executionId,
        Map<String, Object> outputs,
        AgentMetrics metrics,
        List<String> toolsUsed) {
}
