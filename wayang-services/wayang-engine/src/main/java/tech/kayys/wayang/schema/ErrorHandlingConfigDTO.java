package tech.kayys.wayang.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * ErrorHandlingConfigDTO - Error handling configuration
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorHandlingConfigDTO {

    private RetryPolicyDTO retryPolicy;
    private FallbackConfigDTO fallback;
    private CircuitBreakerConfigDTO circuitBreaker;
    private EscalationConfigDTO escalation;
    private HumanReviewConfigDTO humanReview;

    // Getters and setters...
    public RetryPolicyDTO getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(RetryPolicyDTO retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public FallbackConfigDTO getFallback() {
        return fallback;
    }

    public void setFallback(FallbackConfigDTO fallback) {
        this.fallback = fallback;
    }

    public CircuitBreakerConfigDTO getCircuitBreaker() {
        return circuitBreaker;
    }

    public void setCircuitBreaker(CircuitBreakerConfigDTO circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    public EscalationConfigDTO getEscalation() {
        return escalation;
    }

    public void setEscalation(EscalationConfigDTO escalation) {
        this.escalation = escalation;
    }

    public HumanReviewConfigDTO getHumanReview() {
        return humanReview;
    }

    public void setHumanReview(HumanReviewConfigDTO humanReview) {
        this.humanReview = humanReview;
    }
}