package tech.kayys.wayang.agent.dto;

import java.util.Map;

/**
 * Execution Metrics
 */
public record ExecutionMetrics(
    long executionTimeMs,
    int tokensUsed,
    int toolInvocations,
    long memoryUsedBytes,
    double successScore,
    Map<String, Object> customMetrics
) {
    
    public static ExecutionMetrics empty() {
        return new ExecutionMetrics(0L, 0, 0, 0L, 0.0, Map.of());
    }
}

