package tech.kayys.wayang.workflow.multiregion.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Health check response
 */
public record HealthCheckResponse(
        boolean success,
        double latencyMillis,
        double errorRate,
        int activeConnections,
        Map<String, Object> metrics,
        Optional<String> diagnosticMessage) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success = false;
        private double latencyMillis = 0.0;
        private double errorRate = 0.0;
        private int activeConnections = 0;
        private Map<String, Object> metrics = new HashMap<>();
        private Optional<String> diagnosticMessage = Optional.empty();

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder latencyMillis(double latencyMillis) {
            this.latencyMillis = latencyMillis;
            return this;
        }

        public Builder errorRate(double errorRate) {
            this.errorRate = errorRate;
            return this;
        }

        public Builder activeConnections(int activeConnections) {
            this.activeConnections = activeConnections;
            return this;
        }

        public Builder metrics(Map<String, Object> metrics) {
            this.metrics = new HashMap<>(metrics);
            return this;
        }

        public Builder addMetric(String key, Object value) {
            this.metrics.put(key, value);
            return this;
        }

        public Builder diagnosticMessage(String diagnosticMessage) {
            this.diagnosticMessage = Optional.ofNullable(diagnosticMessage);
            return this;
        }

        public HealthCheckResponse build() {
            return new HealthCheckResponse(
                    success,
                    latencyMillis,
                    errorRate,
                    activeConnections,
                    Collections.unmodifiableMap(metrics),
                    diagnosticMessage);
        }
    }
}
