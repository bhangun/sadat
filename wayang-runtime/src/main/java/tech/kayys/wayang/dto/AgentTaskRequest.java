package tech.kayys.wayang.dto;

import java.util.Map;

public record AgentTaskRequest(
        String instruction,
        Map<String, Object> context) {
}
