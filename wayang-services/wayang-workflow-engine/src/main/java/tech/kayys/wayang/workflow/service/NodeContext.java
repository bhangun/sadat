package tech.kayys.wayang.workflow.service;

import lombok.Builder;
import lombok.Data;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.schema.node.NodeDefinition;

import java.util.Map;

/**
 * NodeContext - Enhanced execution context for node execution
 *
 * This context provides all necessary information for node execution
 * in a use case agnostic manner.
 */
@Data
@Builder
public class NodeContext {
    private String nodeId;
    private String runId;
    private String workflowId;
    private String tenantId;
    private String executionId;
    private Map<String, Object> inputs;
    private Map<String, Object> outputs;
    private Map<String, Object> metadata;
    private WorkflowDefinition workflow;
    private NodeDefinition nodeDefinition;
    private Map<String, Object> executionState;
    private String userId;
    private Map<String, String> tags;
    private Long timestamp;
}
