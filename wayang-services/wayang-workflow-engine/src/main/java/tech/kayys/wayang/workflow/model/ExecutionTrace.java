package tech.kayys.wayang.workflow.model;

import java.util.Map;

public class ExecutionTrace {
    private final String nodeId;
    private final String nodeName;
    private final long timestamp;
    private final Map<String, Object> input;
    private final Map<String, Object> output;
    private final String status;

    public ExecutionTrace(String nodeId, String nodeName, long timestamp,
            Map<String, Object> input, Map<String, Object> output,
            String status) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.timestamp = timestamp;
        this.input = input;
        this.output = output;
        this.status = status;
    }

    // Getters
    public String getNodeId() {
        return nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public Map<String, Object> getOutput() {
        return output;
    }

    public String getStatus() {
        return status;
    }
}
