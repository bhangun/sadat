package tech.kayys.wayang.agent;

/**
 * Coordination Strategy
 */
public enum CoordinationStrategy {
    CENTRALIZED,         // Orchestrator controls all
    DECENTRALIZED,       // Agents coordinate directly
    HYBRID,              // Mixed approach
    CONSENSUS,           // Agents reach consensus
    VOTING              // Democratic decision-making
}
