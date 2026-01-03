package tech.kayys.silat.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;

/**
 * Value object representing a workflow identifier.
 */
@EqualsAndHashCode
public final class WorkflowId {

    private final String id;

    private WorkflowId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("WorkflowId cannot be null or empty");
        }
        this.id = id;
    }

    @JsonCreator
    public static WorkflowId of(String id) {
        return new WorkflowId(id);
    }

    @JsonValue
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}