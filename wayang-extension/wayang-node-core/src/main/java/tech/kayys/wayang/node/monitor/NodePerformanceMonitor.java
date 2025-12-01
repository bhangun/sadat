package tech.kayys.wayang.node.core.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitors node performance and provides optimization insights.
 */
@ApplicationScoped
public class NodePerformanceMonitor {
    
    private static final Logger LOG = LoggerFactory.getLogger(NodePerformanceMonitor.class);
    
    private final MeterRegistry meterRegistry;
    private final Map<String, NodePerformanceMetrics> metricsMap;
    
    @Inject
    public NodePerformanceMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.metricsMap = new ConcurrentHashMap<>();
    }
    
    /**
     * Start tracking execution
     */
    public ExecutionTracker startExecution(String nodeId, String executionId) {
        NodePerformanceMetrics metrics = metricsMap.computeIfAbsent(
            nodeId,
            id -> new NodePerformanceMetrics(id)
        );
        
        return new ExecutionTracker(nodeId, executionId, metrics, meterRegistry);
    }
    
    /**
     * Get performance metrics for a node
     */
    public NodePerformanceMetrics getMetrics(String nodeId) {
        return metricsMap.get(nodeId);
    }
    
    /**
     * Get all performance metrics
     */
    public Map<String, NodePerformanceMetrics> getAllMetrics() {
        return Map.copyOf(metricsMap);
    }
    
    /**
     * Get performance summary
     */
    public PerformanceSummary getSummary(String nodeId) {
        NodePerformanceMetrics metrics = metricsMap.get(nodeId);
        if (metrics == null) {
            return null;
        }
        
        long totalExecutions = metrics.successCount.get() + metrics.failureCount.get();
        double successRate = totalExecutions > 0 ?
            (double) metrics.successCount.get() / totalExecutions * 100 : 0;
        
        double avgDuration = metrics.totalDurationMs.get() > 0 && totalExecutions > 0 ?
            (double) metrics.totalDurationMs.get() / totalExecutions : 0;
        
        return new PerformanceSummary(
            nodeId,
            totalExecutions,
            metrics.successCount.get(),
            metrics.failureCount.get(),
            successRate,
            avgDuration,
            metrics.minDurationMs.get(),
            metrics.maxDurationMs.get(),
            metrics.lastExecutionTime
        );
    }
    
    /**
     * Execution tracker
     */
    public static class ExecutionTracker implements AutoCloseable {
        private final String nodeId;
        private final String executionId;
        private final NodePerformanceMetrics metrics;
        private final MeterRegistry meterRegistry;
        private final Instant startTime;
        private final Timer.Sample timerSample;
        
        ExecutionTracker(
            String nodeId,
            String executionId,
            NodePerformanceMetrics metrics,
            MeterRegistry meterRegistry
        ) {
            this.nodeId = nodeId;
            this.executionId = executionId;
            this.metrics = metrics;
            this.meterRegistry = meterRegistry;
            this.startTime = Instant.now();
            this.timerSample = Timer.start(meterRegistry);
        }
        
        /**
         * Record successful execution
         */
        public void recordSuccess() {
            long durationMs = Duration.between(startTime, Instant.now()).toMillis();
            
            metrics.successCount.incrementAndGet();
            metrics.totalDurationMs.addAndGet(durationMs);
            metrics.lastExecutionTime = Instant.now();
            
            updateMinMax(durationMs);
            
            timerSample.stop(Timer.builder("node.execution")
                .tag("node", nodeId)
                .tag("status", "success")
                .register(meterRegistry));
        }
        
        /**
         * Record failed execution
         */
        public void recordFailure() {
            long durationMs = Duration.between(startTime, Instant.now()).toMillis();
            
            metrics.failureCount.incrementAndGet();
            metrics.totalDurationMs.addAndGet(durationMs);
            metrics.lastExecutionTime = Instant.now();
            
            timerSample.stop(Timer.builder("node.execution")
                .tag("node", nodeId)
                .tag("status", "failure")
                .register(meterRegistry));
        }
        
        private void updateMinMax(long durationMs) {
            metrics.minDurationMs.accumulateAndGet(durationMs, Math::min);
            metrics.maxDurationMs.accumulateAndGet(durationMs, Math::max);
        }
        
        @Override
        public void close() {
            // Auto-close support
        }
    }
}