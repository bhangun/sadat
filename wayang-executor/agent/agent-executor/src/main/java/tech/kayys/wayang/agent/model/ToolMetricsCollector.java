package tech.kayys.wayang.agent.model;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Metrics collector for tool executions
 */
@ApplicationScoped
public class ToolMetricsCollector {

    private static final Logger LOG = LoggerFactory.getLogger(ToolMetricsCollector.class);

    private final Map<String, ToolMetrics> metrics = new ConcurrentHashMap<>();

    public void recordExecution(String toolName, boolean success) {
        LOG.debug("Recording execution for tool: {}", toolName);
        metrics.computeIfAbsent(toolName, k -> new ToolMetrics(toolName))
                .recordExecution(success);
    }

    public ToolMetrics getMetrics(String toolName) {
        LOG.debug("Getting metrics for tool: {}", toolName);
        return metrics.get(toolName);
    }

    public Map<String, ToolMetrics> getAllMetrics() {
        LOG.debug("Getting all metrics");
        return new HashMap<>(metrics);
    }

    public static class ToolMetrics {
        private final String toolName;
        private final java.util.concurrent.atomic.AtomicLong totalExecutions = new java.util.concurrent.atomic.AtomicLong();
        private final java.util.concurrent.atomic.AtomicLong successfulExecutions = new java.util.concurrent.atomic.AtomicLong();
        private final java.util.concurrent.atomic.AtomicLong failedExecutions = new java.util.concurrent.atomic.AtomicLong();

        public ToolMetrics(String toolName) {
            this.toolName = toolName;
        }

        public void recordExecution(boolean success) {
            totalExecutions.incrementAndGet();
            if (success) {
                successfulExecutions.incrementAndGet();
            } else {
                failedExecutions.incrementAndGet();
            }
        }

        public Map<String, Object> toMap() {
            return Map.of(
                    "toolName", toolName,
                    "totalExecutions", totalExecutions.get(),
                    "successfulExecutions", successfulExecutions.get(),
                    "failedExecutions", failedExecutions.get(),
                    "successRate", calculateSuccessRate());
        }

        private double calculateSuccessRate() {
            long total = totalExecutions.get();
            if (total == 0)
                return 0.0;
            return (double) successfulExecutions.get() / total * 100.0;
        }
    }
}
