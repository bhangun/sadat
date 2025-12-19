package tech.kayys.wayang.agent.metrics;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.Metric;

import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class MetricsService {

    @Inject
    MetricRegistry metricRegistry;

    private final AtomicLong activeAgents = new AtomicLong(0);
    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong successfulExecutions = new AtomicLong(0);
    private final AtomicLong failedExecutions = new AtomicLong(0);

    public void onStart(@Observes StartupEvent ev) {
        // Register custom gauges
        metricRegistry.register("wayang.agent.active.count", 
            (Gauge<Long>) () -> activeAgents.get());
        metricRegistry.register("wayang.executions.total.count", 
            (Gauge<Long>) () -> totalExecutions.get());
        metricRegistry.register("wayang.executions.successful.count", 
            (Gauge<Long>) () -> successfulExecutions.get());
        metricRegistry.register("wayang.executions.failed.count", 
            (Gauge<Long>) () -> failedExecutions.get());
    }

    public void incrementActiveAgents() {
        activeAgents.incrementAndGet();
    }

    public void decrementActiveAgents() {
        activeAgents.decrementAndGet();
    }

    public void recordExecution(boolean success) {
        totalExecutions.incrementAndGet();
        if (success) {
            successfulExecutions.incrementAndGet();
        } else {
            failedExecutions.incrementAndGet();
        }
    }

    public void recordAgentCreated() {
        metricRegistry.counter("wayang.agent.created.total").inc();
    }

    public void recordAgentExecutionStarted() {
        metricRegistry.counter("wayang.agent.execution.started.total").inc();
    }

    public void recordAgentExecutionCompleted(boolean success) {
        if (success) {
            metricRegistry.counter("wayang.agent.execution.completed.success.total").inc();
        } else {
            metricRegistry.counter("wayang.agent.execution.completed.failure.total").inc();
        }
    }

    public void recordIntegrationCreated() {
        metricRegistry.counter("wayang.integration.created.total").inc();
    }
}