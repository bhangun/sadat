package tech.kayys.wayang.agent.orchestrator.dto;

import java.util.List;

public record ConsensusDecision(
    boolean consensusReached,
    Object decision,
    List<String> dissenting
) {}
