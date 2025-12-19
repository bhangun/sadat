package tech.kayys.wayang.schema;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * HumanReviewConfigDTO - Human review configuration
 */
@RegisterForReflection
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HumanReviewConfigDTO {

    private boolean enabled = false;
    private String thresholdSeverity = "CRITICAL";
    private String reviewQueue;

    // Getters and setters...
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getThresholdSeverity() {
        return thresholdSeverity;
    }

    public void setThresholdSeverity(String thresholdSeverity) {
        this.thresholdSeverity = thresholdSeverity;
    }

    public String getReviewQueue() {
        return reviewQueue;
    }

    public void setReviewQueue(String reviewQueue) {
        this.reviewQueue = reviewQueue;
    }
}