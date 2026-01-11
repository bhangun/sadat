package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Distributed Tracing Integration
 */
interface WorkflowTracingService {

    /**
     * Start trace span for workflow
     */
    io.opentelemetry.api.trace.Span startWorkflowSpan(tech.kayys.silat.core.domain.WorkflowRunId runId);

    /**
     * Start trace span for node
     */
    io.opentelemetry.api.trace.Span startNodeSpan(tech.kayys.silat.core.domain.WorkflowRunId runId, tech.kayys.silat.core.domain.NodeId nodeId);

    /**
     * Add trace attributes
     */
    void addTraceAttribute(String key, String value);

    /**
     * Get trace context
     */
    io.opentelemetry.context.Context getTraceContext();
}