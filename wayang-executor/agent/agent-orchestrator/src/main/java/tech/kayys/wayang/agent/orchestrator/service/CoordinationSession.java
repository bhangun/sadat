package tech.kayys.wayang.agent.orchestrator.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.agent.dto.CoordinationConfig;
import tech.kayys.wayang.agent.dto.CoordinationStatus;
import tech.kayys.wayang.agent.dto.CoordinationType;

public record CoordinationSession(
    String coordinationId,
    CoordinationType type,
    List<String> participatingAgents,
    String goal,
    CoordinationConfig config,
    CoordinationStatus status,
    Map<String, Object> contributions,
    Instant initiatedAt
) {
    void updateStatus(CoordinationStatus newStatus) {
        // Would update in database
    }
}