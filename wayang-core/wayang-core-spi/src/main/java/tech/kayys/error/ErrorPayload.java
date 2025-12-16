package tech.kayys.wayang.plugin.error;

/**
 * Represents an error payload from a node
 */
public class ErrorPayload {
    private String type;
    private String originNode;
    private int attempt;
    private int maxAttempts;
    private boolean retryable;
    private String message;
    private Throwable cause;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOriginNode() {
        return originNode;
    }

    public void setOriginNode(String originNode) {
        this.originNode = originNode;
    }

    public int getAttempt() {
        return attempt;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public void setRetryable(boolean retryable) {
        this.retryable = retryable;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ErrorPayload payload = new ErrorPayload();

        public Builder type(String type) {
            payload.setType(type);
            return this;
        }

        public Builder originNode(String originNode) {
            payload.setOriginNode(originNode);
            return this;
        }

        public Builder attempt(int attempt) {
            payload.setAttempt(attempt);
            return this;
        }

        public Builder maxAttempts(int maxAttempts) {
            payload.setMaxAttempts(maxAttempts);
            return this;
        }

        public Builder retryable(boolean retryable) {
            payload.setRetryable(retryable);
            return this;
        }

        public Builder message(String message) {
            payload.setMessage(message);
            return this;
        }

        public Builder cause(Throwable cause) {
            payload.setCause(cause);
            return this;
        }

        public ErrorPayload build() {
            return payload;
        }
    }
}
