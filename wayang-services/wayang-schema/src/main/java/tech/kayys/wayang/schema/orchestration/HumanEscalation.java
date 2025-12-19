package tech.kayys.wayang.schema.orchestration;

public class HumanEscalation {
    private Double onConfidenceBelow;
    private Boolean onPolicyViolation = true;
    private OrchestrationTarget fallbackTarget;

    public Double getOnConfidenceBelow() {
        return onConfidenceBelow;
    }

    public void setOnConfidenceBelow(Double onConfidenceBelow) {
        if (onConfidenceBelow != null && (onConfidenceBelow < 0 || onConfidenceBelow > 1)) {
            throw new IllegalArgumentException("Confidence threshold must be between 0 and 1");
        }
        this.onConfidenceBelow = onConfidenceBelow;
    }

    public Boolean getOnPolicyViolation() {
        return onPolicyViolation;
    }

    public void setOnPolicyViolation(Boolean onPolicyViolation) {
        this.onPolicyViolation = onPolicyViolation;
    }

    public OrchestrationTarget getFallbackTarget() {
        return fallbackTarget;
    }

    public void setFallbackTarget(OrchestrationTarget fallbackTarget) {
        this.fallbackTarget = fallbackTarget;
    }
}
