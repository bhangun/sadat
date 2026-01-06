package tech.kayys.wayang.project.dto;

import java.util.List;
import java.util.Map;

public record AgentExecutionResult(
        String taskId,
        boolean success,
        String response,
        List<String> actionstaken,
        Map<String, Object> output,
        List<String> errors) {
}
