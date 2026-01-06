package tech.kayys.wayang.agent.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Agent Coordination Result
 */
public record AgentCoordinationResult(
    String coordinationId,
    CoordinationStatus status,
    Object decision,
    Map<String, Object> agentContributions,
    List<String> dissenting,
    Instant completedAt
) {}

