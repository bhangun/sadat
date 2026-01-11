package tech.kayys.wayang.agent.model;

import java.util.List;
import java.util.Map;

/**
 * Response from LLM provider
 */
public record LLMResponse(
        String content,
        String finishReason,
        List<ToolCall> toolCalls,
        TokenUsage usage,
        Map<String, Object> metadata) {

    public LLMResponse {
        toolCalls = toolCalls != null ? List.copyOf(toolCalls) : List.of();
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public static LLMResponse create(
            String content,
            String finishReason,
            TokenUsage usage) {
        return new LLMResponse(content, finishReason, List.of(), usage, Map.of());
    }

    public static LLMResponse withToolCalls(
            String content,
            List<ToolCall> toolCalls,
            TokenUsage usage) {
        return new LLMResponse(content, "tool_calls", toolCalls, usage, Map.of());
    }

    public Message message() {
        if (hasToolCalls()) {
            return new Message("assistant", content, toolCalls, null, java.time.Instant.now());
        } else {
            return Message.assistant(content);
        }
    }
}
