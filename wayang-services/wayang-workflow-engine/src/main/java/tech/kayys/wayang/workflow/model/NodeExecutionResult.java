package tech.kayys.wayang.workflow.model;

import java.util.Map;

public class NodeExecutionResult {
    private final String nodeId;
    private final boolean success;
    private final Map<String, Object> output;
    private final String error;

    public NodeExecutionResult(String nodeId, boolean success,
            Map<String, Object> output, String error) {
        this.nodeId = nodeId;
        this.success = success;
        this.output = output;
        this.error = error;
    }

    public static NodeExecutionResult skip(String nodeId) {
        return new NodeExecutionResult(nodeId, true, Map.of(), null);
    }

    public String getNodeId() {
        return nodeId;
    }

    public boolean isSuccess() {
        return success;
    }

    public Map<String, Object> getOutput() {
        return output;
    }

    public String getError() {
        return error;
    }
}
