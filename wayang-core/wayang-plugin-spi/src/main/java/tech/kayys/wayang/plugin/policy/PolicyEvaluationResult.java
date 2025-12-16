package tech.kayys.wayang.plugin.policy;

import tech.kayys.wayang.plugin.error.ErrorAction;

/**
 * Result of policy evaluation
 */
public class PolicyEvaluationResult {
    private ErrorAction action;
    private String reason;
    private String humanReviewThreshold;
    
    public boolean hasExplicitAction() {
        return action != null;
    }

    public ErrorAction getAction() {
        return action;
    }

    public void setAction(ErrorAction action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getHumanReviewThreshold() {
        return humanReviewThreshold;
    }

    public void setHumanReviewThreshold(String humanReviewThreshold) {
        this.humanReviewThreshold = humanReviewThreshold;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ErrorAction action;
        private String reason;
        private String humanReviewThreshold;

        private Builder() {}

        public Builder withAction(ErrorAction action) {
            this.action = action;
            return this;
        }

        public Builder withReason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder withHumanReviewThreshold(String humanReviewThreshold) {
            this.humanReviewThreshold = humanReviewThreshold;
            return this;
        }

        public PolicyEvaluationResult build() {
            PolicyEvaluationResult result = new PolicyEvaluationResult();
            result.setAction(this.action);
            result.setReason(this.reason);
            result.setHumanReviewThreshold(this.humanReviewThreshold);
            return result;
        }
    }
}
