package tech.kayys.wayang.agent.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.TokenUsage;

/**
 * Collects and aggregates agent execution metrics
 */
@ApplicationScoped
public class AgentMetricsCollector {

    private static final Logger LOG = LoggerFactory.getLogger(AgentMetricsCollector.class);

    private final Map<String, NodeMetrics> nodeMetrics = new ConcurrentHashMap<>();
    private final Map<String, ProviderMetrics> providerMetrics = new ConcurrentHashMap<>();

    /**
     * Record agent execution
     */
    public void recordExecution(String nodeId, Duration duration, boolean success) {
        nodeMetrics.computeIfAbsent(nodeId, k -> new NodeMetrics(nodeId))
                .recordExecution(duration, success);

        LOG.trace("Recorded execution for node: {} ({}ms, {})",
                nodeId, duration.toMillis(), success ? "success" : "failure");
    }

    /**
     * Record token usage
     */
    public void recordTokenUsage(String provider, String model, TokenUsage usage) {
        String key = provider + ":" + model;

        providerMetrics.computeIfAbsent(key, k -> new ProviderMetrics(provider, model))
                .recordTokens(usage);

        LOG.trace("Recorded token usage: {} tokens for {}",
                usage.totalTokens(), key);
    }

    /**
     * Record tool execution
     */
    public void recordToolExecution(String toolName, boolean success) {
        // Delegate to tool metrics collector
        LOG.trace("Recorded tool execution: {} ({})",
                toolName, success ? "success" : "failure");
    }

    /**
     * Get metrics for a specific node
     */
    public NodeMetrics getNodeMetrics(String nodeId) {
        return nodeMetrics.get(nodeId);
    }

    /**
     * Get all node metrics
     */
    public Map<String, NodeMetrics> getAllNodeMetrics() {
        return new HashMap<>(nodeMetrics);
    }

    /**
     * Get provider metrics
     */
    public ProviderMetrics getProviderMetrics(String provider, String model) {
        return providerMetrics.get(provider + ":" + model);
    }

    /**
     * Get all provider metrics
     */
    public Map<String, ProviderMetrics> getAllProviderMetrics() {
        return new HashMap<>(providerMetrics);
    }

    /**
     * Get aggregated statistics
     */
    public AgentStatistics getStatistics() {
        long totalExecutions = nodeMetrics.values().stream()
                .mapToLong(m -> m.getTotalExecutions())
                .sum();

        long successfulExecutions = nodeMetrics.values().stream()
                .mapToLong(m -> m.getSuccessfulExecutions())
                .sum();

        long totalTokens = providerMetrics.values().stream()
                .mapToLong(m -> m.getTotalTokens())
                .sum();

        double avgDuration = nodeMetrics.values().stream()
                .filter(m -> m.getTotalExecutions() > 0)
                .mapToDouble(NodeMetrics::getAverageDuration)
                .average()
                .orElse(0.0);

        return new AgentStatistics(
                totalExecutions,
                successfulExecutions,
                totalTokens,
                avgDuration);
    }

    /**
     * Node-specific metrics
     */
    public static class NodeMetrics {
        private final String nodeId;
        private final AtomicLong totalExecutions = new AtomicLong();
        private final AtomicLong successfulExecutions = new AtomicLong();
        private final AtomicLong failedExecutions = new AtomicLong();
        private final List<Long> durations = new ArrayList<>();

        public NodeMetrics(String nodeId) {
            this.nodeId = nodeId;
        }

        public void recordExecution(Duration duration, boolean success) {
            totalExecutions.incrementAndGet();

            if (success) {
                successfulExecutions.incrementAndGet();
            } else {
                failedExecutions.incrementAndGet();
            }

            synchronized (durations) {
                durations.add(duration.toMillis());

                // Keep only last 1000 measurements
                if (durations.size() > 1000) {
                    durations.remove(0);
                }
            }
        }

        public long getTotalExecutions() {
            return totalExecutions.get();
        }

        public long getSuccessfulExecutions() {
            return successfulExecutions.get();
        }

        public long getFailedExecutions() {
            return failedExecutions.get();
        }

        public double getSuccessRate() {
            long total = totalExecutions.get();
            if (total == 0)
                return 0.0;
            return (double) successfulExecutions.get() / total * 100.0;
        }

        public double getAverageDuration() {
            synchronized (durations) {
                if (durations.isEmpty())
                    return 0.0;
                return durations.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0.0);
            }
        }

        public Map<String, Object> toMap() {
            return Map.of(
                    "nodeId", nodeId,
                    "totalExecutions", getTotalExecutions(),
                    "successfulExecutions", getSuccessfulExecutions(),
                    "failedExecutions", getFailedExecutions(),
                    "successRate", getSuccessRate(),
                    "averageDurationMs", getAverageDuration());
        }
    }

    /**
     * Provider-specific metrics
     */
    public static class ProviderMetrics {
        private final String provider;
        private final String model;
        private final AtomicLong totalRequests = new AtomicLong();
        private final AtomicLong totalPromptTokens = new AtomicLong();
        private final AtomicLong totalCompletionTokens = new AtomicLong();
        private final AtomicLong totalTokens = new AtomicLong();

        public ProviderMetrics(String provider, String model) {
            this.provider = provider;
            this.model = model;
        }

        public void recordTokens(TokenUsage usage) {
            totalRequests.incrementAndGet();
            totalPromptTokens.addAndGet(usage.promptTokens());
            totalCompletionTokens.addAndGet(usage.completionTokens());
            totalTokens.addAndGet(usage.totalTokens());
        }

        public long getTotalRequests() {
            return totalRequests.get();
        }

        public long getTotalTokens() {
            return totalTokens.get();
        }

        public long getAverageTokensPerRequest() {
            long requests = totalRequests.get();
            if (requests == 0)
                return 0;
            return totalTokens.get() / requests;
        }

        public Map<String, Object> toMap() {
            return Map.of(
                    "provider", provider,
                    "model", model,
                    "totalRequests", getTotalRequests(),
                    "totalPromptTokens", totalPromptTokens.get(),
                    "totalCompletionTokens", totalCompletionTokens.get(),
                    "totalTokens", getTotalTokens(),
                    "averageTokensPerRequest", getAverageTokensPerRequest());
        }
    }
}
