package tech.kayys.wayang.project.dto;

import java.util.List;
import java.util.Map;

public record AgentTask(
        String taskId,
        String instruction,
        Map<String, Object> context,
        List<String> requiredCapabilities) {
}
