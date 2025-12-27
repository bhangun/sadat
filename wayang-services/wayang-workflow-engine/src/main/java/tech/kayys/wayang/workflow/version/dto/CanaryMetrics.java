package tech.kayys.wayang.workflow.version.dto;

public record CanaryMetrics(
        int totalExecutions,
        int successfulExecutions,
        int failedExecutions,
        double averageLatency,
        double errorRate) {
    public boolean isHealthy() {
        return errorRate < 0.05; // Less than 5% error rate
    }
}
