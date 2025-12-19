package tech.kayys.wayang.workflow.service;

import lombok.Builder;
import lombok.Data;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;

import java.util.Map;

@Data
@Builder
public class NodeContext {
    private String nodeId;
    private String runId;
    private String tenantId;
    private Map<String, Object> inputs;
    private Map<String, Object> metadata;
    private WorkflowDefinition workflow;
}
