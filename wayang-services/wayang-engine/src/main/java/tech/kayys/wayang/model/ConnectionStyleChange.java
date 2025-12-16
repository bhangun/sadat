package tech.kayys.wayang.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * ConnectionStyleChange - Connection visual style change
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectionStyleChange {

    public String connectionId;
    public String property; // e.g., "color", "pathStyle"
    public String oldValue;
    public String newValue;
    public Instant timestamp = Instant.now();

    public String getDescription() {
        return String.format("Connection '%s' %s changed from '%s' to '%s'",
                connectionId, property, oldValue, newValue);
    }
}
