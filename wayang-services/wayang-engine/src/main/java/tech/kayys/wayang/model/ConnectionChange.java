package tech.kayys.wayang.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * ConnectionChange - Represents a change to a connection/edge
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectionChange {

    public ChangeType type;

    // Connection identity
    public String connectionId;
    public String from;
    public String to;
    public String fromPort;
    public String toPort;

    // Change details
    public ConnectionDefinition oldValue;
    public ConnectionDefinition newValue;
    public List<String> modifications; // Specific fields changed

    // Metadata
    public Instant timestamp = Instant.now();
    public String changedBy;

    // Impact
    public ImpactAssessment impact;

    /**
     * Get human-readable description
     */
    public String getDescription() {
        return switch (type) {
            case ADDED -> String.format("Added connection: %s[%s] → %s[%s]",
                    from, fromPort, to, toPort);
            case DELETED -> String.format("Deleted connection: %s[%s] → %s[%s]",
                    from, fromPort, to, toPort);
            case MODIFIED -> String.format("Modified connection: %s[%s] → %s[%s]: %s",
                    from, fromPort, to, toPort, String.join(", ", modifications));
        };
    }

    /**
     * Check if this affects data flow
     */
    public boolean affectsDataFlow() {
        return type == ChangeType.DELETED ||
                (type == ChangeType.MODIFIED &&
                        (modifications.contains("condition") || modifications.contains("fromPort") ||
                                modifications.contains("toPort")));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ConnectionChange that = (ConnectionChange) o;
        return Objects.equals(connectionId, that.connectionId) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectionId, type);
    }
}
