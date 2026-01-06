package tech.kayys.wayang.workflow.kernel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;

import java.util.UUID;

/**
 * Value object representing a unique workflow run identifier.
 * Immutable and serializable.
 */
@EqualsAndHashCode
public final class WorkflowRunId {

    private final String id;

    private WorkflowRunId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("WorkflowRunId cannot be null or empty");
        }
        this.id = id;
    }

    @JsonCreator
    public static WorkflowRunId of(String id) {
        return new WorkflowRunId(id);
    }

    public static WorkflowRunId generate() {
        return new WorkflowRunId(UUID.randomUUID().toString());
    }

    @JsonValue
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }

    public boolean isValid() {
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
