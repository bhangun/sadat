
/**
 * Quota limits configuration
 */
record QuotaLimits(
    long maxExecutions,
    long maxCpuTimeMs,
    long maxMemoryBytes,
    long maxTokens,
    double maxCostUsd
) {
    public static QuotaLimits defaultLimits() {
        return new QuotaLimits(
            10000,              // 10k executions
            3600000,            // 1 hour CPU time
            10L * 1024 * 1024 * 1024, // 10 GB memory
            1000000,            // 1M tokens
            100.0               // $100
        );
    }
    
    public static QuotaLimits unlimited() {
        return new QuotaLimits(
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            Double.MAX_VALUE
        );
    }
}