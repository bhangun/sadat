package tech.kayys.wayang.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * CircuitBreakerConfigDTO - Circuit breaker configuration
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CircuitBreakerConfigDTO {

    private boolean enabled = false;
    private int failureThreshold = 5;
    private int successThreshold = 2;
    private int timeoutMs = 60000;
    private int windowMs = 600000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public int getSuccessThreshold() {
        return successThreshold;
    }

    public void setSuccessThreshold(int successThreshold) {
        this.successThreshold = successThreshold;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getWindowMs() {
        return windowMs;
    }

    public void setWindowMs(int windowMs) {
        this.windowMs = windowMs;
    }
}