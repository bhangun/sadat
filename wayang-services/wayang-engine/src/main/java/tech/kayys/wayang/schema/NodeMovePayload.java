package tech.kayys.wayang.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * NodeMovePayload - Node movement data
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeMovePayload {

    @NotBlank
    private String nodeId;

    @NotNull
    private PointDTO position;

    // Getters and setters...
    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public PointDTO getPosition() {
        return position;
    }

    public void setPosition(PointDTO position) {
        this.position = position;
    }
}