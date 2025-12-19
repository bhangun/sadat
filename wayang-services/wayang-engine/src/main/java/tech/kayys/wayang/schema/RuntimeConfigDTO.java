package tech.kayys.wayang.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * RuntimeConfigDTO - Runtime configuration
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuntimeConfigDTO {

    private String mode = "sync"; // sync, async, stream
    private RetryPolicyDTO retryPolicy;
    private Integer timeoutMs;
    private ResourceProfileDTO resourceProfile;

    // Getters and setters...
    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public RetryPolicyDTO getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(RetryPolicyDTO retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public ResourceProfileDTO getResourceProfile() {
        return resourceProfile;
    }

    public void setResourceProfile(ResourceProfileDTO resourceProfile) {
        this.resourceProfile = resourceProfile;
    }
}
