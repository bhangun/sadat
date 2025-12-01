/**
 * Execution metrics
 */
record ExecutionMetrics(
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