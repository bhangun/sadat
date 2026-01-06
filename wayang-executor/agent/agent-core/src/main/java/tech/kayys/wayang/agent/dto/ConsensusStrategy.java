package tech.kayys.wayang.agent.dto;

/**
 * Consensus Strategy
 */
public enum ConsensusStrategy {
    UNANIMOUS,       // All agree
    MAJORITY,        // >50% agree
    SUPERMAJORITY,   // >66% agree
    WEIGHTED,        // Weighted voting
    LEADER_DECIDES   // Leader makes final call
}
