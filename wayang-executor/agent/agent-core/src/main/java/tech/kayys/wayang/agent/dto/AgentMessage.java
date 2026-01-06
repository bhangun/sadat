package tech.kayys.wayang.agent.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Agent Message
 */
public record AgentMessage(
    String messageId,
    String fromAgentId,
    String toAgentId,
    MessageType type,
    String content,
    Map<String, Object> payload,
    String conversationId,
    Instant sentAt
) {}
