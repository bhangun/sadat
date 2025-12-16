package tech.kayys.wayang.plugin;

import java.time.Instant;
import java.util.Map;

/**
 * Health Check Result
 */
public class HealthCheckResult {
    

    private boolean healthy = true;
    
    private String message;
    private Map<String, Object> details;
    private Instant timestamp;
    
    public static HealthCheckResult healthy(String message) {
        return HealthCheckResult.builder()
            .healthy(true)
            .message(message)
            .timestamp(Instant.now())
            .build();
    }
    
    public static HealthCheckResult unhealthy(String message, Map<String, Object> details) {
        return HealthCheckResult.builder()
            .healthy(false)
            .message(message)
            .details(details)
            .timestamp(Instant.now())
            .build();
    }

    // Getters
    public boolean isHealthy() {
        return healthy;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    // Setters
    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    // Builder
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean healthy = true;
        private String message;
        private Map<String, Object> details;
        private Instant timestamp;

        public Builder healthy(boolean healthy) {
            this.healthy = healthy;
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

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public HealthCheckResult build() {
            HealthCheckResult result = new HealthCheckResult();
            result.healthy = this.healthy;
            result.message = this.message;
            result.details = this.details;
            result.timestamp = this.timestamp;
            return result;
        }
    }
}