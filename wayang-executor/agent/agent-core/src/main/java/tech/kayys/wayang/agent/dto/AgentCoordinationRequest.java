package tech.kayys.wayang.agent.dto;

import java.time.Instant;
import java.util.List;

/**
 * Agent Coordination Request
 */
public record AgentCoordinationRequest(
    String coordinationId,
    CoordinationType type,
    List<String> participatingAgents,
    String goal,
    CoordinationConfig config,
    Instant initiatedAt
) {}
