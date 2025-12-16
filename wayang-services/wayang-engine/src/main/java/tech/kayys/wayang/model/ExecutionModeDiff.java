package tech.kayys.wayang.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * ExecutionModeDiff - Execution mode change details
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionModeDiff {
    public String oldMode;
    public String newMode;

    public String getDescription() {
        return String.format("Execution mode changed from '%s' to '%s'", oldMode, newMode);
    }
}
