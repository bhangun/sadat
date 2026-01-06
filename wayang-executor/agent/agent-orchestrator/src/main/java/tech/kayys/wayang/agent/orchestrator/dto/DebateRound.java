package tech.kayys.wayang.agent.orchestrator.dto;

import java.util.Map;

public record DebateRound(
    int roundNumber,
    Map<String, String> positions,
    String moderatorSummary
) {}
