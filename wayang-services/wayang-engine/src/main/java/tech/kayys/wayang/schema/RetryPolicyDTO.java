package tech.kayys.wayang.schema;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * RetryPolicyDTO - Retry policy configuration
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RetryPolicyDTO {

    private int maxAttempts = 3;
    private int initialDelayMs = 200;
    private int maxDelayMs = 30000;
    private String backoff = "exponential"; // fixed, exponential, linear
    private boolean jitter = true;
    private List<String> retryOn = new ArrayList<>();

    // Getters and setters...
    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getInitialDelayMs() {
        return initialDelayMs;
    }

    public void setInitialDelayMs(int initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }

    public int getMaxDelayMs() {
        return maxDelayMs;
    }

    public void setMaxDelayMs(int maxDelayMs) {
        this.maxDelayMs = maxDelayMs;
    }

    public String getBackoff() {
        return backoff;
    }

    public void setBackoff(String backoff) {
        this.backoff = backoff;
    }

    public boolean isJitter() {
        return jitter;
    }

    public void setJitter(boolean jitter) {
        this.jitter = jitter;
    }

    public List<String> getRetryOn() {
        return retryOn;
    }

    public void setRetryOn(List<String> retryOn) {
        this.retryOn = retryOn;
    }
}
