package tech.kayys.wayang.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * RuntimeDiff - Runtime configuration changes
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuntimeDiff {

    public boolean changed = false;
    public boolean modeChanged = false;
    public boolean retriesChanged = false;
    public boolean timeoutChanged = false;
    public boolean resourceProfileChanged = false;

    // Specific changes
    public ExecutionModeDiff modeDiff;
    public RetryPolicyDiff retryDiff;
    public ResourceProfileDiff resourceDiff;

    /**
     * Check if changes require redeployment
     */
    public boolean requiresRedeployment() {
        return modeChanged || resourceProfileChanged;
    }
}
