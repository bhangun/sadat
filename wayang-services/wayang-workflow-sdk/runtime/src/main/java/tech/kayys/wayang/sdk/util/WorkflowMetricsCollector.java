package tech.kayys.wayang.sdk.util;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;





import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;

/**
 * Metrics collection for workflow SDK operations
 */
@ApplicationScoped
public class WorkflowMetricsCollector {

    @Inject
    MeterRegistry registry;

    /**
     * Record workflow trigger event
     */
    public void recordWorkflowTriggered(String workflowId, String tenantId) {
        Counter.builder("workflow.sdk.triggered")
            .tag("workflow_id", workflowId)
            .tag("tenant_id", tenantId)
            .register(registry)
            .increment();
    }

    /**
     * Record workflow execution duration
     */
    public void recordWorkflowDuration(
        String workflowId, 
        Duration duration, 
        String status
    ) {
        Timer.builder("workflow.sdk.duration")
            .tag("workflow_id", workflowId)
            .tag("status", status)
            .register(registry)
            .record(duration);
    }

    /**
     * Record API call
     */
    public void recordApiCall(String operation, boolean success, Duration duration) {
        Timer.builder("workflow.sdk.api.call")
            .tag("operation", operation)
            .tag("status", success ? "success" : "failure")
            .register(registry)
            .record(duration);
    }

    /**
     * Record API error
     */
    public void recordApiError(String operation, String errorType) {
        Counter.builder("workflow.sdk.api.errors")
            .tag("operation", operation)
            .tag("error_type", errorType)
            .register(registry)
            .increment();
    }
}
