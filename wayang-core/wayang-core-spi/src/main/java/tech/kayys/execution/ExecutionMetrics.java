package tech.kayys.execution;

import java.time.Duration;
import java.util.Map;

/**
 * Execution metrics
 */
public record ExecutionMetrics(
    Duration duration,
    long memoryUsedBytes,
    long cpuTimeMillis,
    int tokensConsumed,
    double costUsd,
    Map<String, Number> customMetrics
) {
    public ExecutionMetrics {
        customMetrics = customMetrics != null ? Map.copyOf(customMetrics) : Map.of();
    }
}