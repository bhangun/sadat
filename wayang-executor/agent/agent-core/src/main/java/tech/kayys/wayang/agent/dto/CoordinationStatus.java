package tech.kayys.wayang.agent.dto;

/**
 * Coordination Status
 */
public enum CoordinationStatus {
    IN_PROGRESS,
    CONSENSUS_REACHED,
    NO_CONSENSUS,
    TIMEOUT,
    FAILED
}
