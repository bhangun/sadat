package tech.kayys.wayang.agent.orchestrator.model;

public enum FailureType {
    TIMEOUT,
    EXECUTION_ERROR,
    INSUFFICIENT_CAPABILITY,
    RESOURCE_UNAVAILABLE,
    FATAL_ERROR
}