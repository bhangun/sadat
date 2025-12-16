package tech.kayys.wayang.plugin;

import java.util.Map;

/**
 * Plugin Metrics
 */
public class PluginMetrics {
    
    
    private long requestCount = 0;
    
    
    private long errorCount = 0;
    
    
    private double averageLatencyMs = 0.0;
    
    private Double p95LatencyMs;
    private Double p99LatencyMs;
    
    
    private long activeRequests = 0;
    
    private Map<String, Long> nodeExecutionCounts;
    private Map<String, Double> nodeAverageLatencies;

    public long getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(long requestCount) {
        this.requestCount = requestCount;
    }

    public long getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }

    public double getAverageLatencyMs() {
        return averageLatencyMs;
    }

    public void setAverageLatencyMs(double averageLatencyMs) {
        this.averageLatencyMs = averageLatencyMs;
    }

    public Double getP95LatencyMs() {
        return p95LatencyMs;
    }

    public void setP95LatencyMs(Double p95LatencyMs) {
        this.p95LatencyMs = p95LatencyMs;
    }

    public Double getP99LatencyMs() {
        return p99LatencyMs;
    }

    public void setP99LatencyMs(Double p99LatencyMs) {
        this.p99LatencyMs = p99LatencyMs;
    }

    public long getActiveRequests() {
        return activeRequests;
    }

    public void setActiveRequests(long activeRequests) {
        this.activeRequests = activeRequests;
    }

    public Map<String, Long> getNodeExecutionCounts() {
        return nodeExecutionCounts;
    }

    public void setNodeExecutionCounts(Map<String, Long> nodeExecutionCounts) {
        this.nodeExecutionCounts = nodeExecutionCounts;
    }

    public Map<String, Double> getNodeAverageLatencies() {
        return nodeAverageLatencies;
    }

    public void setNodeAverageLatencies(Map<String, Double> nodeAverageLatencies) {
        this.nodeAverageLatencies = nodeAverageLatencies;
    }


    public static Builder builder() {
        return new Builder();
    }
    public static class Builder {
        private PluginMetrics metrics = new PluginMetrics();

        public Builder requestCount(long requestCount) {
            metrics.setRequestCount(requestCount);
            return this;
        }

        public Builder errorCount(long errorCount) {
            metrics.setErrorCount(errorCount);
            return this;
        }

        public Builder averageLatencyMs(double averageLatencyMs) {
            metrics.setAverageLatencyMs(averageLatencyMs);
            return this;
        }

        public Builder p95LatencyMs(Double p95LatencyMs) {
            metrics.setP95LatencyMs(p95LatencyMs);
            return this;
        }

        public Builder p99LatencyMs(Double p99LatencyMs) {
            metrics.setP99LatencyMs(p99LatencyMs);
            return this;
        }

        public Builder activeRequests(long activeRequests) {
            metrics.setActiveRequests(activeRequests);
            return this;
        }

        public Builder nodeExecutionCounts(Map<String, Long> nodeExecutionCounts) {
            metrics.setNodeExecutionCounts(nodeExecutionCounts);
            return this;
        }

        public Builder nodeAverageLatencies(Map<String, Double> nodeAverageLatencies) {
            metrics.setNodeAverageLatencies(nodeAverageLatencies);
            return this;
        }

        public PluginMetrics build() {
            return metrics;
        }
    }
}