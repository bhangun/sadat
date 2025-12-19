package tech.kayys.wayang.workflow.api.model;

import java.time.Instant;
import java.util.Map;

/**
 * Workflow Event (immutable)
 */
public record WorkflowEvent(
        String id,
        String runId,
        Long sequence,
        WorkflowEventType type,
        Map<String, Object> data,
        Instant timestamp) {
}
