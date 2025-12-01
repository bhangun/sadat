package tech.kayys.wayang.node.core.exception;

import tech.kayys.wayang.node.core.isolation.TenantUsageSnapshot;

/**
 * Exception thrown when resource quotas are exceeded.
 */
public class QuotaExceededException extends NodeException {
    
    private final TenantUsageSnapshot usage;
    
    public QuotaExceededException(String message) {
        this(message, null);
    }
    
    public QuotaExceededException(String message, TenantUsageSnapshot usage) {
        super(message, "QUOTA_EXCEEDED", false);
        this.usage = usage;
    }
    
    public TenantUsageSnapshot getUsage() {
        return usage;
    }
    
    /**
     * Get detailed quota information
     */
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        
        if (usage != null) {
            sb.append("\nQuota usage details:");
            sb.append("\n  Executions: ").append(usage.getExecutionUsagePercent()).append("%");
            sb.append("\n  CPU: ").append(usage.getCpuUsagePercent()).append("%");
            sb.append("\n  Memory: ").append(usage.getMemoryUsagePercent()).append("%");
            sb.append("\n  Tokens: ").append(usage.getTokenUsagePercent()).append("%");
            sb.append("\n  Cost: $").append(usage.totalCostUsd());
        }
        
        return sb.toString();
    }
}