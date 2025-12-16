

package tech.kayys.wayang.observability.service;

import io.micrometer.core.instrument.*;
import io.opentelemetry.api.trace.*;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class MetricsService {
    
    @Inject
    MeterRegistry meterRegistry;
    
    @Inject
    Tracer tracer;
    
    private final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    
    /**
     * Record node execution metrics
     */
    @ConsumeEvent("node.completed")
    public void recordNodeExecution(NodeCompletedEvent event) {
        Timer timer = timers.computeIfAbsent(
            "node.execution." + event.nodeType(),
            name -> Timer.builder(name)
                .description("Node execution duration")
                .tag("nodeType", event.nodeType())
                .tag("tenant", event.tenantId())
                .register(meterRegistry)
        );
        
        timer.record(Duration.ofMillis(event.durationMs()));
        
        // Record success/failure
        Counter statusCounter = counters.computeIfAbsent(
            "node.status." + event.status(),
            name -> Counter.builder(name)
                .description("Node execution status")
                .tag("status", event.status().toString())
                .tag("nodeType", event.nodeType())
                .register(meterRegistry)
        );
        
        statusCounter.increment();
    }
    
    /**
     * Record LLM token usage
     */
    public void recordTokenUsage(
        String modelId, 
        String tenantId, 
        int tokensIn, 
        int tokensOut
    ) {
        Counter.builder("llm.tokens.input")
            .description("LLM input tokens")
            .tag("model", modelId)
            .tag("tenant", tenantId)
            .register(meterRegistry)
            .increment(tokensIn);
        
        Counter.builder("llm.tokens.output")
            .description("LLM output tokens")
            .tag("model", modelId)
            .tag("tenant", tenantId)
            .register(meterRegistry)
            .increment(tokensOut);
    }
    
    /**
     * Record cost
     */
    public void recordCost(String tenantId, String service, double costUSD) {
        Counter.builder("platform.cost.usd")
            .description("Platform cost in USD")
            .tag("tenant", tenantId)
            .tag("service", service)
            .register(meterRegistry)
            .increment(costUSD);
    }
    
    /**
     * Create distributed trace span
     */
    public Span createSpan(String spanName, String runId, String nodeId) {
        return tracer.spanBuilder(spanName)
            .setAttribute("run.id", runId)
            .setAttribute("node.id", nodeId)
            .startSpan();
    }
}