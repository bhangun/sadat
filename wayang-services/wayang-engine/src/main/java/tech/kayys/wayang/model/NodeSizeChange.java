package tech.kayys.wayang.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * NodeSizeChange - Node size change
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeSizeChange {

    public String nodeId;
    public UIDefinition.Size oldSize;
    public UIDefinition.Size newSize;
    public Instant timestamp = Instant.now();

    public String getDescription() {
        return String.format("Node '%s' resized from %.0fx%.0f to %.0fx%.0f",
                nodeId, oldSize.width, oldSize.height, newSize.width, newSize.height);
    }
}
