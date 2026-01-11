package tech.kayys.wayang.agent.model;

import java.time.Instant;
import java.util.List;

/**
 * Represents a chat message with role and content
 * Supports tool calls and tool results
 */
public record Message(
        String role,
        String content,
        List<ToolCall> toolCalls,
        String toolCallId,
        Instant timestamp) {

    public Message {
        toolCalls = toolCalls != null ? List.copyOf(toolCalls) : null;
        timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public static Message system(String content) {
        return new Message("system", content, null, null, Instant.now());
    }

    public static Message user(String content) {
        return new Message("user", content, null, null, Instant.now());
    }

    public static Message assistant(String content) {
        return new Message("assistant", content, null, null, Instant.now());
    }

    public static Message assistant(String content, List<ToolCall> toolCalls) {
        return new Message("assistant", content, toolCalls, null, Instant.now());
    }

    public static Message tool(String toolCallId, String content) {
        return new Message("tool", content, null, toolCallId, Instant.now());
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    public boolean isSystem() {
        return "system".equals(role);
    }

    public boolean isUser() {
        return "user".equals(role);
    }

    public boolean isAssistant() {
        return "assistant".equals(role);
    }

    public boolean isTool() {
        return "tool".equals(role);
    }
}
