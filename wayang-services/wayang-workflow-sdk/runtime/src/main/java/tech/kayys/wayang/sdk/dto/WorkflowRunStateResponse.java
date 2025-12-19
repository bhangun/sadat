package tech.kayys.wayang.sdk.dto;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;




/**
 * Detailed workflow run state
 */
public record WorkflowRunStateResponse(
    String runId,
    RunStatus status,
    Map<String, NodeExecutionState> nodeStates,
    Map<String, Object> workflowState,
    List<String> executionPath,
    Map<String, Object> output
) {
    public NodeExecutionState getNodeState(String nodeId) {
        return nodeStates.get(nodeId);
    }

    public boolean hasNodeExecuted(String nodeId) {
        NodeExecutionState state = nodeStates.get(nodeId);
        return state != null && state.status() != NodeExecutionState.NodeStatus.PENDING;
    }
}
