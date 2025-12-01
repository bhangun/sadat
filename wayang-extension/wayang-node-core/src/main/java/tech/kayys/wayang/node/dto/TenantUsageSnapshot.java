
/**
 * Tenant usage snapshot
 */
record TenantUsageSnapshot(
    String tenantId,
    long executionCount,
    long totalCpuTimeMs,
    long totalMemoryBytes,
    long totalTokens,
    double totalCostUsd,
    QuotaLimits limits,
    Instant resetTime
) {
    public double getExecutionUsagePercent() {
        return (double) executionCount / limits.maxExecutions * 100;
    }
    
    public double getCpuUsagePercent() {
        return (double) totalCpuTimeMs / limits.maxCpuTimeMs * 100;
    }
    
    public double getMemoryUsagePercent() {
        return (double) totalMemoryBytes / limits.maxMemoryBytes * 100;
    }
    
    public double getTokenUsagePercent() {
        return (double) totalTokens / limits.maxTokens * 100;
    }
    
    public double getCostUsagePercent() {
        return totalCostUsd / limits.maxCostUsd * 100;
    }
}