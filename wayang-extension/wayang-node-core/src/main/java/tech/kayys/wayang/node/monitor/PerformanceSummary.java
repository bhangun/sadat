/**
 * Performance summary
 */
record PerformanceSummary(
    String nodeId,
    long totalExecutions,
    long successfulExecutions,
    long failedExecutions,
    double successRate,
    double avgDurationMs,
    long minDurationMs,
    long maxDurationMs,
    Instant lastExecutionTime
) {}