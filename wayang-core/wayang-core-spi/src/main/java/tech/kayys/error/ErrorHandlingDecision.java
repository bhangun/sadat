package tech.kayys.wayang.plugin.error;


import java.util.Map;

import tech.kayys.wayang.plugin.policy.PolicyEvaluationResult;


/**
 * Error Handling Decision
 */
public class ErrorHandlingDecision {
    
 
    private ErrorAction action;
    
    private String reason;
    private Map<String, Object> metadata;
    private boolean shouldRetry;
    private boolean shouldEscalate;
    private long delayMs;
    private String hitlTaskId;
    private Object fixedInput;
    
    public static ErrorHandlingDecision delegate() {
        return ErrorHandlingDecision.builder()
            .action(ErrorAction.DELEGATE_TO_PLATFORM)
            .build();
    }
    
    public static ErrorHandlingDecision retry(int maxAttempts) {
        return ErrorHandlingDecision.builder()
            .action(ErrorAction.RETRY)
            .metadata(Map.of("maxAttempts", maxAttempts))
            .build();
    }
    
    public static ErrorHandlingDecision escalate(String reason) {
        return ErrorHandlingDecision.builder()
            .action(ErrorAction.ESCALATE)
            .reason(reason)
            .build();
    }

    public static ErrorHandlingDecision fromPolicy(PolicyEvaluationResult result) {
        return builder()
            .action(result.getAction())
            .reason(result.getReason())
            .build();
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public boolean isShouldRetry() {
        return shouldRetry;
    }

    public void setShouldRetry(boolean shouldRetry) {
        this.shouldRetry = shouldRetry;
    }

    public boolean isShouldEscalate() {
        return shouldEscalate;
    }

    public void setShouldEscalate(boolean shouldEscalate) {
        this.shouldEscalate = shouldEscalate;
    }

    public long getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }

    public String getHitlTaskId() {
        return hitlTaskId;
    }

    public void setHitlTaskId(String hitlTaskId) {
        this.hitlTaskId = hitlTaskId;
    }

    public Object getFixedInput() {
        return fixedInput;
    }

    public void setFixedInput(Object fixedInput) {
        this.fixedInput = fixedInput;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ErrorAction action;
        private String reason;
        private Map<String, Object> metadata;
        private boolean shouldRetry;
        private boolean shouldEscalate;
        private long delayMs;
        private String hitlTaskId;
        private Object fixedInput;

        public Builder action(ErrorAction action) {
            this.action = action;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder shouldRetry(boolean shouldRetry) {
            this.shouldRetry = shouldRetry;
            return this;
        }

        public Builder shouldEscalate(boolean shouldEscalate) {
            this.shouldEscalate = shouldEscalate;
            return this;
        }

        public Builder delayMs(long delayMs) {
            this.delayMs = delayMs;
            return this;
        }

        public Builder hitlTaskId(String hitlTaskId) {
            this.hitlTaskId = hitlTaskId;
            return this;
        }

        public Builder fixedInput(Object fixedInput) {
            this.fixedInput = fixedInput;
            return this;
        }

        public ErrorHandlingDecision build() {
            ErrorHandlingDecision decision = new ErrorHandlingDecision();
            decision.action = this.action;
            decision.reason = this.reason;
            decision.metadata = this.metadata;
            decision.shouldRetry = this.shouldRetry;
            decision.shouldEscalate = this.shouldEscalate;
            decision.delayMs = this.delayMs;
            decision.hitlTaskId = this.hitlTaskId;
            decision.fixedInput = this.fixedInput;
            return decision;
        }
    }
}
