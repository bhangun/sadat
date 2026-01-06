package tech.kayys.wayang.workflow.sdk;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class WorkflowEvent {
    private String eventId;
    private String eventType;
    private String runId;
    private String workflowId;
    private String tenantId;
    private Instant timestamp;
    private Map<String, Object> payload;
    private Map<String, Object> metadata;

    public enum Type {
        RUN_CREATED,
        RUN_STARTED,
        RUN_COMPLETED,
        RUN_FAILED,
        RUN_CANCELLED,
        RUN_WAITING,
        RUN_RESUMED,
        NODE_STARTED,
        NODE_COMPLETED,
        NODE_FAILED,
        NODE_WAITING,
        STATE_UPDATED,
        SIGNAL_RECEIVED,
        ERROR_OCCURRED
    }
}
