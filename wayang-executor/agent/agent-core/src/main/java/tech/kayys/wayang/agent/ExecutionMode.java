package tech.kayys.wayang.agent;

/**
 * Execution Mode
 */
public enum ExecutionMode {
    SEQUENTIAL,          // Execute one by one
    PARALLEL,            // Execute concurrently
    ADAPTIVE,            // Decide based on context
    PIPELINE             // Stream processing
}
