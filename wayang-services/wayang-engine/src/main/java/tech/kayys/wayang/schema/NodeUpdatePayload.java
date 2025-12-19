package tech.kayys.wayang.schema;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * NodeUpdatePayload - Node update data
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeUpdatePayload {

    @NotBlank
    private String nodeId;

    @NotNull
    private Map<String, Object> changes = new HashMap<>();

    // Getters and setters...
    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Map<String, Object> getChanges() {
        return changes;
    }

    public void setChanges(Map<String, Object> changes) {
        this.changes = changes;
    }
}
