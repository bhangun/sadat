package tech.kayys.wayang.workflow.sdk;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder(toBuilder = true)
public class WorkflowRun {
    private String runId;
    private String workflowId;
    private String workflowVersion;
    private String tenantId;
    private RunStatus status;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant updatedAt;
    private Map<String, Object> inputs;
    private Map<String, Object> outputs;
    private Map<String, Object> workflowState;
    private String triggeredBy;
    private String errorMessage;
    private List<NodeExecutionState> nodeStates;
    private Map<String, Object> metadata;

    public enum RunStatus {
        CREATED,
        RUNNING,
        WAITING,
        RETRYING,
        COMPENSATING,
        COMPLETED,
        FAILED,
        CANCELLED,
        PAUSED,
        TIMED_OUT
    }
}
