package tech.kayys.wayang.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;

/**
 * ConnectionInput - Input for connection operations
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectionInput {

    @NotBlank
    private String from;

    @NotBlank
    private String to;

    @NotBlank
    private String fromPort;

    @NotBlank
    private String toPort;

    private String condition;

    // Getters and setters...
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
}
