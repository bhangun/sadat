package tech.kayys.wayang.schema;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;

/**
 * ConnectionDTO - Connection between nodes
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectionDTO {

    @NotBlank
    private String id;

    @NotBlank
    private String from;

    @NotBlank
    private String to;

    @NotBlank
    private String fromPort;

    @NotBlank
    private String toPort;

    private String condition; // CEL expression

    private Map<String, Object> metadata = new HashMap<>();

    // Getters and setters...
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getFromPort() {
        return fromPort;
    }

    public void setFromPort(String fromPort) {
        this.fromPort = fromPort;
    }

    public String getToPort() {
        return toPort;
    }

    public void setToPort(String toPort) {
        this.toPort = toPort;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
