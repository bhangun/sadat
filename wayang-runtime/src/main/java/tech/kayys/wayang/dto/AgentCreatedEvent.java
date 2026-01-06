package tech.kayys.wayang.dto;

import java.time.Instant;
import java.util.UUID;

import tech.kayys.wayang.project.dto.AgentType;

public record AgentCreatedEvent(
                UUID agentId,
                String agentName,
                AgentType agentType,
                Instant timestamp) {
}
