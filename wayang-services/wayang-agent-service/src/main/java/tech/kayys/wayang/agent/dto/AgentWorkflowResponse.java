package tech.kayys.wayang.agent.dto;

import java.time.Instant;

public record AgentWorkflowResponse(
        String id,
        String name,
        String version,
        String status,
        Instant createdAt) {
}
