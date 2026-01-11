package tech.kayys.wayang.agent.model;

import java.util.Map;

/**
 * Tool definition for LLM function calling
 */
public record ToolDefinition(
        String name,
        String description,
        Map<String, Object> parameters) {

    public ToolDefinition {
        parameters = Map.copyOf(parameters != null ? parameters : Map.of());
    }

    public static ToolDefinition create(
            String name,
            String description,
            Map<String, Object> parameters) {
        return new ToolDefinition(name, description, parameters);
    }
}
