package tech.kayys.wayang.agent.model;

import java.util.Map;

/**
 * Tool call requested by LLM
 */
public record ToolCall(
        String id,
        String name,
        Map<String, Object> arguments) {

    public ToolCall {
        arguments = Map.copyOf(arguments != null ? arguments : Map.of());
    }

    public static ToolCall create(String id, String name, Map<String, Object> arguments) {
        return new ToolCall(id, name, arguments);
    }
}
