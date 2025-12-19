package tech.kayys.wayang.workflow.service;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.domain.WorkflowRun;

/**
 * Telemetry service interface.
 */
@ApplicationScoped
public class TelemetryService {

    public void recordWorkflowStart(WorkflowRun run) {
        // Implementation
    }

    public void recordWorkflowCompletion(WorkflowRun run, long durationMs) {
        // Implementation
    }

    public void recordNodeExecution(String nodeId, String type, long duration, Object status) {
        // Implementation
    }
}
