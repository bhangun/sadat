package tech.kayys.wayang.agent.orchestrator.dto;

import java.util.List;

public record DebateSession(
    String debateId,
    List<String> participants,
    String topic,
    int currentRound,
    List<DebateRound> rounds
) {}
