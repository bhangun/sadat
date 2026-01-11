package tech.kayys.wayang.agent.model;

import java.util.Map;

/**
 * Result of tool execution
 */
public record ToolResult(
        String id,
        String name,
        String output,
        boolean success,
        String error,
        Map<String, Object> metadata) {

    public ToolResult {
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public static ToolResult success(String id, String name, String output) {
        return new ToolResult(id, name, output, true, null, Map.of());
    }

    public static ToolResult error(String id, String name, String error) {
        return new ToolResult(id, name, null, false, error, Map.of());
    }
}
