package tech.kayys.wayang.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * NodeStyleChange - Node visual style change
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeStyleChange {

    public String nodeId;
    public String property; // e.g., "color", "icon", "shape"
    public String oldValue;
    public String newValue;
    public Instant timestamp = Instant.now();

    public String getDescription() {
        return String.format("Node '%s' %s changed from '%s' to '%s'",
                nodeId, property, oldValue, newValue);
    }
}