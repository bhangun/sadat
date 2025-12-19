package tech.kayys.wayang.agent.dto;

import java.time.Instant;

public record OrchestratorResponse(
        String id,
        String name,
        int subAgentCount,
        Instant createdAt) {
}
