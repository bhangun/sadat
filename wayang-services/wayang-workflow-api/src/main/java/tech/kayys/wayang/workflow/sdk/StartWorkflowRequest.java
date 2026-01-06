package tech.kayys.wayang.workflow.sdk;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class StartWorkflowRequest {
    private String workflowId;
    private String workflowVersion; // Optional, uses latest if null
    private String tenantId;
    private Map<String, Object> inputs;
    private Map<String, Object> workflowState;
    private String triggeredBy; // Optional, defaults to "client-sdk"
    private Map<String, Object> metadata;
    private ExecutionOptions executionOptions;

    @Data
    @Builder
    public static class ExecutionOptions {
        private Boolean dryRun;
        private Boolean validateOnly;
        private String executionStrategy;
        private Integer priority;
        private Map<String, Object> overrides;
    }
}
