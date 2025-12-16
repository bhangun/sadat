
package tech.kayys.wayang.plugin.node;

import jakarta.inject.Inject;
import jakarta.enterprise.inject.spi.CDI;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import tech.kayys.wayang.plugin.ExecutionResult;
import java.util.concurrent.TimeUnit;

/**
 * Metrics collector
 */
public class MetricsCollector {
    
    @Inject
    MeterRegistry registry;
    
    private final String nodeId;
    private final Counter executionCounter;
    private final Timer executionTimer;
    private final Counter failureCounter;
    
    private MetricsCollector(String nodeId, MeterRegistry registry) {
        this.nodeId = nodeId;
        this.executionCounter = registry.counter("node.executions", "node", nodeId);
        this.executionTimer = registry.timer("node.duration", "node", nodeId);
        this.failureCounter = registry.counter("node.failures", "node", nodeId);
    }
    
    public static MetricsCollector forNode(String nodeId) {
        MeterRegistry registry = CDI.current().select(MeterRegistry.class).get();
        if (registry == null) {
            registry = new SimpleMeterRegistry();
        }
        return new MetricsCollector(nodeId, registry);
    }
    
    public void recordExecution(long durationNanos, ExecutionResult.Status status) {
        executionCounter.increment();
        executionTimer.record(durationNanos, TimeUnit.NANOSECONDS);
        
        if (status == ExecutionResult.Status.FAILED) {
            failureCounter.increment();
        }
    }
    
    public void recordFailure(Throwable throwable) {
        failureCounter.increment();
    }
    
    public void close() {
        // Cleanup if needed
    }
}
