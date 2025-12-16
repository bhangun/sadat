package tech.kayys.wayang.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * NodeChange - Represents a change to a node
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeChange {

    public ChangeType type;

    // Node identity
    public String nodeId;
    public String nodeName;
    public String nodeType;

    // Change details
    public NodeDefinition oldValue;
    public NodeDefinition newValue;
    public List<String> modifications; // Specific fields changed

    // Metadata
    public Instant timestamp = Instant.now();
    public String changedBy;
    public String changeReason;

    // Impact analysis
    public ImpactAssessment impact;

    /**
     * Get human-readable description
     */
    public String getDescription() {
        return switch (type) {
            case ADDED -> String.format("Added node '%s' (%s)", nodeName, nodeType);
            case DELETED -> String.format("Deleted node '%s' (%s)", nodeName, nodeType);
            case MODIFIED -> String.format("Modified node '%s' (%s): %s",
                    nodeName, nodeType, String.join(", ", modifications));
        };
    }

    /**
     * Check if this is a breaking change
     */
    public boolean isBreaking() {
        return type == ChangeType.DELETED ||
                (type == ChangeType.MODIFIED &&
                        (modifications.contains("inputs") || modifications.contains("outputs")));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NodeChange that = (NodeChange) o;
        return Objects.equals(nodeId, that.nodeId) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, type);
    }
}
