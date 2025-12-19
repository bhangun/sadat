package tech.kayys.wayang.schema;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * CollaborationEvent - Event sent to clients
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollaborationEvent {

    @NotNull
    private EventType type;

    @NotBlank
    private String userId;

    @NotBlank
    private String workflowId;

    @NotNull
    private Object payload;

    private Instant timestamp = Instant.now();

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final CollaborationEvent event = new CollaborationEvent();

        public Builder type(EventType type) {
            event.type = type;
            return this;
        }

        public Builder userId(String userId) {
            event.userId = userId;
            return this;
        }

        public Builder workflowId(String workflowId) {
            event.workflowId = workflowId;
            return this;
        }

        public Builder payload(Object payload) {
            event.payload = payload;
            return this;
        }

        public CollaborationEvent build() {
            return event;
        }
    }

    // Getters and setters...
    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
