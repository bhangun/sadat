package tech.kayys.wayang.workflow.sdk;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class WorkflowProgress {
    private String runId;
    private String workflowId;
    private RunStatus status;
    private Instant timestamp;
    private Double progress; // 0.0 to 1.0
    private String currentActivity;
    private List<String> completedNodes;
    private List<String> pendingNodes;
    private Map<String, Object> currentState;
    private String message;
    private Map<String, Object> metrics;
}
