package tech.kayys.wayang.schema;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * ExecutionError - Error information
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionError {

    private String code;
    private String message;
    private String nodeId;
    private Map<String, Object> details = new HashMap<>();

    // Getters and setters...
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }
}