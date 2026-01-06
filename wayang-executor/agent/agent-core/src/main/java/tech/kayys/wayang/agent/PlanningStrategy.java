package tech.kayys.wayang.agent;

/**
 * Planning Strategies
 */
public enum PlanningStrategy {
    HIERARCHICAL,          // Top-down decomposition
    CHAIN_OF_THOUGHT,      // Step-by-step reasoning
    TREE_OF_THOUGHT,       // Multiple reasoning paths
    REACT,                 // Reasoning + Acting cycles
    PLAN_AND_EXECUTE,      // Plan first, execute later
    ADAPTIVE              // Dynamic replanning
}
