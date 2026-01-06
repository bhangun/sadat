package tech.kayys.wayang.workflow.kernel;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.sdk.dto.ExecutionMetrics;
import tech.kayys.wayang.workflow.kernel.NodeDescriptor;
import tech.kayys.wayang.workflow.kernel.PerformanceMarker;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.hibernate.engine.config.spi.ConfigurationService;

/**
 * Collects and aggregates execution metrics
 */
@ApplicationScoped
public class ExecutionMetricsCollector {

    private final Map<String, NodeMetrics> nodeMetrics = new ConcurrentHashMap<>();
    private final Map<String, RunMetrics> runMetrics = new ConcurrentHashMap<>();
    private final LongAdder totalRequests = new LongAdder();
    private final LongAdder successfulRequests = new LongAdder();
    private final LongAdder failedRequests = new LongAdder();
    private final Map<String, LongAdder> errorCounts = new ConcurrentHashMap<>();
    private final List<PerformanceMarker> performanceMarkers = Collections.synchronizedList(new ArrayList<>());

    @Inject
    MetricsPublisher metricsPublisher;

    @Inject
    ConfigurationService configService;

    public void recordNodeExecution(String nodeId, String nodeType,
            long durationMs, boolean success) {
        totalRequests.increment();

        String key = nodeType + ":" + nodeId;
        NodeMetrics metrics = nodeMetrics.computeIfAbsent(key, k -> new NodeMetrics(nodeId, nodeType));

        metrics.recordExecution(durationMs, success);

        if (success) {
            successfulRequests.increment();
        } else {
            failedRequests.increment();
        }

        // Publish to external metrics system if configured
        if (configService.isMetricsPublishingEnabled()) {
            metricsPublisher.publishNodeMetrics(nodeId, nodeType, durationMs, success);
        }
    }

    public void recordFailure(String nodeId, String nodeType, Throwable throwable) {
        String errorType = throwable.getClass().getSimpleName();
        errorCounts.computeIfAbsent(errorType, k -> new LongAdder()).increment();

        // Also record in node metrics
        String key = nodeType + ":" + nodeId;
        NodeMetrics metrics = nodeMetrics.get(key);
        if (metrics != null) {
            metrics.recordError(errorType);
        }

        if (configService.isMetricsPublishingEnabled()) {
            metricsPublisher.publishError(nodeId, nodeType, errorType);
        }
    }

    public void recordCustomMetric(String metricName, double value, Map<String, String> tags) {
        // Store in appropriate aggregation
        if (configService.isMetricsPublishingEnabled()) {
            metricsPublisher.publishCustomMetric(metricName, value, tags);
        }
    }

    public void addPerformanceMarker(PerformanceMarker marker) {
        performanceMarkers.add(marker);

        // Keep only recent markers to avoid memory issues
        if (performanceMarkers.size() > 1000) {
            performanceMarkers.remove(0);
        }
    }

    public void startRun(String runId, String workflowId) {
        RunMetrics metrics = new RunMetrics(runId, workflowId);
        metrics.start();
        runMetrics.put(runId, metrics);
    }

    public void endRun(String runId, boolean success) {
        RunMetrics metrics = runMetrics.get(runId);
        if (metrics != null) {
            metrics.end(success);

            if (configService.isMetricsPublishingEnabled()) {
                metricsPublisher.publishRunMetrics(metrics);
            }
        }
    }

    public ExecutionMetrics collectForNode(String nodeId) {
        // Find metrics for this node (might be multiple entries with different types)
        List<NodeMetrics> matching = nodeMetrics.values().stream()
                .filter(m -> m.nodeId.equals(nodeId))
                .toList();

        if (matching.isEmpty()) {
            return ExecutionMetrics.empty();
        }

        // Aggregate across all node types
        long totalExecutions = matching.stream().mapToLong(m -> m.executionCount.get()).sum();
        long successfulExecutions = matching.stream().mapToLong(m -> m.successfulExecutions.get()).sum();
        double avgDuration = matching.stream()
                .mapToDouble(m -> m.totalDuration.get() / Math.max(1, m.executionCount.get()))
                .average()
                .orElse(0.0);

        Map<String, Long> errors = new HashMap<>();
        matching.forEach(m -> m.errorCounts.forEach((error, count) -> errors.merge(error, count.get(), Long::sum)));

        return new ExecutionMetrics(totalExecutions, successfulExecutions, avgDuration, errors);
    }

    public ExecutionMetrics collectForNodeType(String nodeType) {
        List<NodeMetrics> matching = nodeMetrics.values().stream()
                .filter(m -> m.nodeType.equals(nodeType))
                .toList();

        if (matching.isEmpty()) {
            return ExecutionMetrics.empty();
        }

        long totalExecutions = matching.stream().mapToLong(m -> m.executionCount.get()).sum();
        long successfulExecutions = matching.stream().mapToLong(m -> m.successfulExecutions.get()).sum();
        double avgDuration = matching.stream()
                .mapToDouble(m -> m.totalDuration.get() / Math.max(1, m.executionCount.get()))
                .average()
                .orElse(0.0);

        Map<String, Long> errors = new HashMap<>();
        matching.forEach(m -> m.errorCounts.forEach((error, count) -> errors.merge(error, count.get(), Long::sum)));

        return new ExecutionMetrics(totalExecutions, successfulExecutions, avgDuration, errors);
    }

    public List<PerformanceMarker> getPerformanceMarkers(String runId) {
        return performanceMarkers.stream()
                .filter(m -> m.getMetadata().containsKey("runId") &&
                        m.getMetadata().get("runId").equals(runId))
                .toList();
    }

    public void clearOldMetrics(Duration retention) {
        Instant cutoff = Instant.now().minus(retention);

        // Clear old run metrics
        runMetrics.entrySet().removeIf(entry -> entry.getValue().endTime != null &&
                entry.getValue().endTime.isBefore(cutoff));

        // Clear old performance markers
        performanceMarkers.removeIf(marker -> marker.getTimestamp().isBefore(cutoff));
    }

    // Getters for aggregated metrics
    public long getTotalRequests() {
        return totalRequests.sum();
    }

    public long getSuccessfulRequests() {
        return successfulRequests.sum();
    }

    public long getFailedRequests() {
        return failedRequests.sum();
    }

    public double getAverageLatencyMs() {
        long totalDuration = nodeMetrics.values().stream()
                .mapToLong(m -> m.totalDuration.get())
                .sum();
        long totalExecutions = nodeMetrics.values().stream()
                .mapToLong(m -> m.executionCount.get())
                .sum();
        return totalExecutions > 0 ? (double) totalDuration / totalExecutions : 0.0;
    }

    public Map<String, Long> getErrorDistribution() {
        Map<String, Long> distribution = new HashMap<>();
        errorCounts.forEach((error, counter) -> distribution.put(error, counter.sum()));
        return distribution;
    }

    private static class NodeMetrics {
        final String nodeId;
        final String nodeType;
        final AtomicLong executionCount = new AtomicLong();
        final AtomicLong successfulExecutions = new AtomicLong();
        final AtomicLong totalDuration = new AtomicLong(); // milliseconds
        final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();

        NodeMetrics(String nodeId, String nodeType) {
            this.nodeId = nodeId;
            this.nodeType = nodeType;
        }

        void recordExecution(long durationMs, boolean success) {
            executionCount.incrementAndGet();
            totalDuration.addAndGet(durationMs);
            if (success) {
                successfulExecutions.incrementAndGet();
            }
        }

        void recordError(String errorType) {
            errorCounts.computeIfAbsent(errorType, k -> new AtomicLong()).incrementAndGet();
        }
    }

    private static class RunMetrics {
        final String runId;
        final String workflowId;
        final Instant startTime;
        Instant endTime;
        boolean success;
        final List<String> executedNodes = Collections.synchronizedList(new ArrayList<>());
        final Map<String, Long> nodeDurations = new ConcurrentHashMap<>();

        RunMetrics(String runId, String workflowId) {
            this.runId = runId;
            this.workflowId = workflowId;
            this.startTime = Instant.now();
        }

        void start() {
            // Already started in constructor
        }

        void end(boolean success) {
            this.endTime = Instant.now();
            this.success = success;
        }

        void recordNodeExecution(String nodeId, long durationMs) {
            executedNodes.add(nodeId);
            nodeDurations.put(nodeId, durationMs);
        }

        Duration getDuration() {
            if (endTime == null) {
                return Duration.between(startTime, Instant.now());
            }
            return Duration.between(startTime, endTime);
        }
    }
}