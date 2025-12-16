package tech.kayys.wayang.plugin;

import java.util.List;

/**
 * Readiness Check Result
 */
public class ReadinessCheckResult {
    
    private boolean ready = true;
    
    private String message;
    private List<String> pendingDependencies;
    
    public static ReadinessCheckResult ready(String message) {
        return ReadinessCheckResult.builder()
            .ready(true)
            .message(message)
            .build();
    }
    
    public static ReadinessCheckResult notReady(String message, List<String> pending) {
        return ReadinessCheckResult.builder()
            .ready(false)
            .message(message)
            .pendingDependencies(pending)
            .build();
    }

    // Getters
    public boolean isReady() {
        return ready;
    }

    public String getMessage() {
        return message;
    }

    public List<String> getPendingDependencies() {
        return pendingDependencies;
    }

    // Setters
    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setPendingDependencies(List<String> pendingDependencies) {
        this.pendingDependencies = pendingDependencies;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean ready = true;
        private String message;
        private List<String> pendingDependencies;

        public Builder ready(boolean ready) {
            this.ready = ready;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder pendingDependencies(List<String> pendingDependencies) {
            this.pendingDependencies = pendingDependencies;
            return this;
        }

        public ReadinessCheckResult build() {
            ReadinessCheckResult result = new ReadinessCheckResult();
            result.ready = this.ready;
            result.message = this.message;
            result.pendingDependencies = this.pendingDependencies;
            return result;
        }
    }
}
