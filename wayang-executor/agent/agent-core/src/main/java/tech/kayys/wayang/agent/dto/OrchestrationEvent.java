package tech.kayys.wayang.agent.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Orchestration Event
 */
public record OrchestrationEvent(
    String eventId,
    OrchestrationEventType type,
    String description,
    Map<String, Object> data,
    Instant occurredAt
) {}
