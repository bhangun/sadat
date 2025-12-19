package tech.kayys.wayang.schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;

/**
 * NodeDTO - Node definition
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeDTO {

    @NotBlank
    private String id;

    @NotBlank
    private String type;

    @NotBlank
    private String name;

    private Map<String, Object> properties = new HashMap<>();

    private List<PortDescriptorDTO> inputs = new ArrayList<>();
    private List<PortDescriptorDTO> outputs = new ArrayList<>();

    // UI position (denormalized for convenience)
    private PointDTO position;

    // Collaboration
    private boolean locked = false;
    private String lockedBy;
    private Instant lockedAt;

    // Metadata
    private String description;
    private Map<String, Object> metadata = new HashMap<>();

    // Error handling
    private ErrorHandlingConfigDTO errorHandling;

    // Getters and setters...
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public List<PortDescriptorDTO> getInputs() {
        return inputs;
    }

    public void setInputs(List<PortDescriptorDTO> inputs) {
        this.inputs = inputs;
    }

    public List<PortDescriptorDTO> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<PortDescriptorDTO> outputs) {
        this.outputs = outputs;
    }

    public PointDTO getPosition() {
        return position;
    }

    public void setPosition(PointDTO position) {
        this.position = position;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(Instant lockedAt) {
        this.lockedAt = lockedAt;
    }

    public ErrorHandlingConfigDTO getErrorHandling() {
        return errorHandling;
    }

    public void setErrorHandling(ErrorHandlingConfigDTO errorHandling) {
        this.errorHandling = errorHandling;
    }
}