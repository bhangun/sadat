package tech.kayys.wayang.workflow.kernel;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class TriggerWorkflowRequest {
    private String workflowId;
    private String workflowVersion;
    private String tenantId;
    private Map<String, Object> inputs;
    private String correlationId;
    private Map<String, Object> metadata;

    public static TriggerWorkflowRequest simple(String workflowId, Map<String, Object> inputs) {
        return TriggerWorkflowRequest.builder()
                .workflowId(workflowId)
                .inputs(inputs)
                .build();
    }
}
