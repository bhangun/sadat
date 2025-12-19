package tech.kayys.wayang.schema.execution;

import java.util.Arrays;
import java.util.List;

public class ErrorHandlingConfig {
    private RetryPolicy retryPolicy;
    private String fallbackNodeId;
    private String humanReviewThreshold = "CRITICAL";
    private Boolean autoHealEnabled = false;
    private CircuitBreaker circuitBreaker;

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public String getFallbackNodeId() {
        return fallbackNodeId;
    }

    public void setFallbackNodeId(String fallbackNodeId) {
        this.fallbackNodeId = fallbackNodeId;
    }

    public String getHumanReviewThreshold() {
        return humanReviewThreshold;
    }

    public void setHumanReviewThreshold(String humanReviewThreshold) {
        List<String> validThresholds = Arrays.asList("NEVER", "INFO", "WARNING", "ERROR", "CRITICAL");
        if (!validThresholds.contains(humanReviewThreshold)) {
            throw new IllegalArgumentException("Invalid human review threshold: " + humanReviewThreshold);
        }
        this.humanReviewThreshold = humanReviewThreshold;
    }

    public Boolean getAutoHealEnabled() {
        return autoHealEnabled;
    }

    public void setAutoHealEnabled(Boolean autoHealEnabled) {
        this.autoHealEnabled = autoHealEnabled;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public void setCircuitBreaker(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }
}
