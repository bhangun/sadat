package tech.kayys.wayang.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * FallbackConfigDTO - Fallback configuration
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FallbackConfigDTO {

    private String type = "none"; // node, static, none
    private String nodeId;
    private Object staticResponse;

    // Getters and setters...
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Object getStaticResponse() {
        return staticResponse;
    }

    public void setStaticResponse(Object staticResponse) {
        this.staticResponse = staticResponse;
    }
}