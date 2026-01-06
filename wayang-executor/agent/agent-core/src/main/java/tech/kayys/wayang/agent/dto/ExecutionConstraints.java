package tech.kayys.wayang.agent.dto;

import java.util.Map;
import java.util.Set;

/**
 * Execution Constraints
 */
public record ExecutionConstraints(
    long maxExecutionTimeMs,
    int maxRetries,
    long maxMemoryBytes,
    Set<String> allowedTools,
    Map<String, Object> customConstraints
) {
    
    public static ExecutionConstraints createDefault() {
        return new ExecutionConstraints(
            300000L,     // 5 minutes
            3,           // 3 retries
            1073741824L, // 1GB
            Set.of(),    // All tools allowed
            Map.of()
        );
    }
}
