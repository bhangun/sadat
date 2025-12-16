package tech.kayys.wayang.model;

import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * NodePositionChange - Node position change
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodePositionChange {

    public String nodeId;
    public UIDefinition.Point oldPosition;
    public UIDefinition.Point newPosition;
    public Instant timestamp = Instant.now();

    /**
     * Calculate distance moved
     */
    public double distanceMoved() {
        if (oldPosition == null || newPosition == null) {
            return 0.0;
        }
        double dx = newPosition.x - oldPosition.x;
        double dy = newPosition.y - oldPosition.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Get human-readable description
     */
    public String getDescription() {
        return String.format("Node '%s' moved from (%.0f, %.0f) to (%.0f, %.0f) - distance: %.0fpx",
                nodeId, oldPosition.x, oldPosition.y, newPosition.x, newPosition.y, distanceMoved());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        NodePositionChange that = (NodePositionChange) o;
        return Objects.equals(nodeId, that.nodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId);
    }
}
