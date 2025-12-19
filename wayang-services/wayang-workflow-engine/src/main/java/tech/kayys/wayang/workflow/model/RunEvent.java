package tech.kayys.wayang.workflow.model;

import java.time.Instant;
import java.util.Map;

/**
 * Run event structure
 */
@lombok.Data
@lombok.Builder
public class RunEvent {
    private String eventType;
    private String runId;
    private String workflowId;
    private String tenantId;
    private String status;
    private Instant timestamp;
    private Map<String, Object> metadata;
}
