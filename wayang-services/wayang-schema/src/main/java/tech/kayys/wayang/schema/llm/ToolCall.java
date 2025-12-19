package tech.kayys.wayang.schema.llm;

import java.util.Map;

/**
 * Tool call.
 */
public class ToolCall {
    private String id;
    private String name;
    private Map<String, Object> arguments;
}
