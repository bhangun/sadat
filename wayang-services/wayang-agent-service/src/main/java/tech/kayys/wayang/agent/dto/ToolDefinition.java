package tech.kayys.wayang.agent.dto;

import java.util.Map;

public record ToolDefinition(
        String id,
        String name,
        String description,
        String category,
        Map<String, Object> parameters) {
}