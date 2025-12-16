package tech.kayys.wayang.collab;

import java.util.Map;

/**
 * WorkflowOperation - Workflow modification operation
 */
public class WorkflowOperation {
    private OperationType type;
    private String targetId;
    private Map<String, Object> data;
    private int version; // For OT

    public enum OperationType {
        NODE_ADD,
        NODE_DELETE,
        NODE_MOVE,
        NODE_UPDATE,
        CONNECTION_ADD,
        CONNECTION_DELETE,
        PROPERTY_UPDATE
    }

    // Getters and setters...
    public OperationType getType() {
        return type;
    }

    public void setType(OperationType type) {
        this.type = type;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
