package tech.kayys.wayang.agent.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * OpenTelemetry tracing instrumentation
 */
@ApplicationScoped
public class AgentTracingInstrumentation {

    private static final Logger LOG = LoggerFactory.getLogger(AgentTracingInstrumentation.class);

    private final Tracer tracer;

    @Inject
    public AgentTracingInstrumentation(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Create span for agent execution
     */
    public Span startExecutionSpan(String runId, String nodeId, String tenantId) {
        Span span = tracer.spanBuilder("agent.execution")
                .setAttribute("run.id", runId)
                .setAttribute("node.id", nodeId)
                .setAttribute("tenant.id", tenantId)
                .startSpan();

        LOG.trace("Started execution span: {}", span.getSpanContext().getSpanId());
        return span;
    }

    /**
     * Create span for LLM call
     */
    public Span startLLMSpan(String provider, String model, int messageCount) {
        Span span = tracer.spanBuilder("agent.llm.call")
                .setAttribute("llm.provider", provider)
                .setAttribute("llm.model", model)
                .setAttribute("llm.message_count", messageCount)
                .startSpan();

        return span;
    }

    /**
     * Create span for tool execution
     */
    public Span startToolSpan(String toolName, Map<String, Object> arguments) {
        Span span = tracer.spanBuilder("agent.tool.call")
                .setAttribute("tool.name", toolName)
                .setAttribute("tool.argument_count", arguments.size())
                .startSpan();

        return span;
    }

    /**
     * Create span for memory operation
     */
    public Span startMemorySpan(String operation, String sessionId) {
        Span span = tracer.spanBuilder("agent.memory." + operation)
                .setAttribute("session.id", sessionId)
                .startSpan();

        return span;
    }

    /**
     * Add event to current span
     */
    public void addEvent(String name, Map<String, String> attributes) {
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.isRecording()) {
            currentSpan.addEvent(name,
                    io.opentelemetry.api.common.Attributes.builder()
                            .putAll(convertToAttributes(attributes))
                            .build());
        }
    }

    /**
     * Record exception in current span
     */
    public void recordException(Throwable exception) {
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.isRecording()) {
            currentSpan.recordException(exception);
            currentSpan.setStatus(StatusCode.ERROR, exception.getMessage());
        }
    }

    /**
     * Execute with tracing
     */
    public <T> Uni<T> traceExecution(
            String spanName,
            Map<String, String> attributes,
            java.util.function.Supplier<Uni<T>> execution) {

        Span span = tracer.spanBuilder(spanName)
                .setAllAttributes(convertToAttributes(attributes))
                .startSpan();

        try (var scope = span.makeCurrent()) {
            return execution.get()
                    .onItem().invoke(item -> {
                        span.setStatus(StatusCode.OK);
                        span.end();
                    })
                    .onFailure().invoke(error -> {
                        span.recordException(error);
                        span.setStatus(StatusCode.ERROR, error.getMessage());
                        span.end();
                    });
        }
    }

    private io.opentelemetry.api.common.Attributes convertToAttributes(
            Map<String, String> map) {

        var builder = io.opentelemetry.api.common.Attributes.builder();
        map.forEach(builder::put);
        return builder.build();
    }
}
