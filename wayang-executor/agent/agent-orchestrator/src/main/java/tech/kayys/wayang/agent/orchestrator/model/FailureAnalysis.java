package tech.kayys.wayang.agent.orchestrator.model;

import java.util.List;

import tech.kayys.wayang.agent.dto.ExecutionError;

public record FailureAnalysis(
    FailureType type,
    boolean isRecoverable,
    List<String> alternativeApproaches,
    List<ExecutionError> errors
) {}
