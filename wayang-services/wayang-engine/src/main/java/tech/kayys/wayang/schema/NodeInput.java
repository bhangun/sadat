package tech.kayys.wayang.schema;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;

/**
 * NodeInput - Input for node operations
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeInput {

    @NotBlank
    private String type;

    @NotBlank
    private String name;

    private Map<String, Object> properties = new HashMap<>();
    private PointDTO position;
    private Map<String, Object> metadata = new HashMap<>();

    // Getters and setters...
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public PointDTO getPosition() {
        return position;
    }

    public void setPosition(PointDTO position) {
        this.position = position;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
