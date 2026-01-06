package tech.kayys.wayang.agent;

/**
 * Orchestration Type
 */
public enum OrchestrationType {
    SEQUENTIAL,           // One agent at a time
    PARALLEL,             // Multiple agents concurrently
    HIERARCHICAL,         // Tree-like delegation
    COLLABORATIVE,        // Agents work together
    COMPETITIVE,          // Best result wins
    DEBATE               // Agents debate solutions
}
