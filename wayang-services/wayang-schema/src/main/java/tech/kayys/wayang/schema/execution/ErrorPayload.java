package tech.kayys.wayang.schema.execution;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Standardized error payload used across the entire platform.
 * Provides rich context for error handling, retry logic, and human escalation.
 * 
 * Design principles:
 * - Uniform structure for all error types
 * - Supports retry decision making
 * - Includes context for debugging
 * - Tamper-proof via hash (optional)
 */
public class ErrorPayload {

    private ErrorType type;
    private String message;
    private Map<String, Object> details = new HashMap<>();
    private Boolean retryable = false;
    private String originNode;
    private String originRunId;
    private Integer attempt = 0;
    private Integer maxAttempts = 3;
    private LocalDateTime timestamp = LocalDateTime.now();
    private SuggestedAction suggestedAction;
    private String provenanceRef;
    private String hash;
    private String stackTrace;

    public ErrorPayload() {
    }

    public ErrorPayload(ErrorType type, String message, Map<String, Object> details, Boolean retryable,
            String originNode, String originRunId, Integer attempt, Integer maxAttempts, LocalDateTime timestamp,
            SuggestedAction suggestedAction, String provenanceRef, String hash, String stackTrace) {
        this.type = type;
        this.message = message;
        this.details = details != null ? details : new HashMap<>();
        this.retryable = retryable != null ? retryable : false;
        this.originNode = originNode;
        this.originRunId = originRunId;
        this.attempt = attempt != null ? attempt : 0;
        this.maxAttempts = maxAttempts != null ? maxAttempts : 3;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
        this.suggestedAction = suggestedAction;
        this.provenanceRef = provenanceRef;
        this.hash = hash;
        this.stackTrace = stackTrace;
    }

    public static ErrorPayload fromThrowable(Throwable th, String nodeId, java.util.UUID runId) { 
        return builder() 
            .type(ErrorType.UNKNOWN_ERROR) 
            .message(th.getMessage()) 
            .originNode(nodeId) 
            .originRunId(runId.toString()) 
            .retryable(false) 
            .suggestedAction(SuggestedAction.HUMAN_REVIEW) 
            .build(); 
    } 

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ErrorType type;
        private String message;
        private Map<String, Object> details = new HashMap<>();
        private Boolean retryable = false;
        private String originNode;
        private String originRunId;
        private Integer attempt = 0;
        private Integer maxAttempts = 3;
        private LocalDateTime timestamp = LocalDateTime.now();
        private SuggestedAction suggestedAction;
        private String provenanceRef;
        private String hash;
        private String stackTrace;

        public Builder type(ErrorType type) {
            this.type = type;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder details(Map<String, Object> details) {
            this.details = details;
            return this;
        }

        public Builder retryable(Boolean retryable) {
            this.retryable = retryable;
            return this;
        }

        public Builder originNode(String originNode) {
            this.originNode = originNode;
            return this;
        }

        public Builder originRunId(String originRunId) {
            this.originRunId = originRunId;
            return this;
        }

        public Builder attempt(Integer attempt) {
            this.attempt = attempt;
            return this;
        }

        public Builder maxAttempts(Integer maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder suggestedAction(SuggestedAction suggestedAction) {
            this.suggestedAction = suggestedAction;
            return this;
        }

        public Builder provenanceRef(String provenanceRef) {
            this.provenanceRef = provenanceRef;
            return this;
        }

        public Builder hash(String hash) {
            this.hash = hash;
            return this;
        }

        public ErrorPayload build() {
            return new ErrorPayload(type, message, details, retryable, originNode, originRunId, attempt, maxAttempts,
                    timestamp, suggestedAction, provenanceRef, hash, stackTrace);
        }
    }

    public boolean isRetryable() {
        return retryable;
    }

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    // Getters and Setters
    public ErrorType getType() {
        return type;
    }

    public void setType(ErrorType type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public Boolean getRetryable() {
        return retryable;
    }

    public void setRetryable(Boolean retryable) {
        this.retryable = retryable;
    }

    public String getOriginNode() {
        return originNode;
    }

    public void setOriginNode(String originNode) {
        this.originNode = originNode;
    }

    public String getOriginRunId() {
        return originRunId;
    }

    public void setOriginRunId(String originRunId) {
        this.originRunId = originRunId;
    }

    public Integer getAttempt() {
        return attempt;
    }

    public void setAttempt(Integer attempt) {
        this.attempt = attempt;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public SuggestedAction getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(SuggestedAction suggestedAction) {
        this.suggestedAction = suggestedAction;
    }

    public String getProvenanceRef() {
        return provenanceRef;
    }

    public void setProvenanceRef(String provenanceRef) {
        this.provenanceRef = provenanceRef;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public enum ErrorType {
        TOOL_ERROR,
        LLM_ERROR,
        NETWORK_ERROR,
        VALIDATION_ERROR,
        TIMEOUT,
        RESOURCE_EXHAUSTED,
        POLICY_VIOLATION,
        UNKNOWN_ERROR
    }

    public enum SuggestedAction {
        RETRY,
        FALLBACK,
        ESCALATE,
        HUMAN_REVIEW,
        ABORT,
        AUTO_FIX
    }

    public boolean canRetry() {
        return Boolean.TRUE.equals(retryable) && attempt < maxAttempts;
    }

    public void incrementAttempt() {
        this.attempt++;
    }

    public boolean requiresHumanReview() {
        return suggestedAction == SuggestedAction.HUMAN_REVIEW ||
                suggestedAction == SuggestedAction.ESCALATE;
    }
}