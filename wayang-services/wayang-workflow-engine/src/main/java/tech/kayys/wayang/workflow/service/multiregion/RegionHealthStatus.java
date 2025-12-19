package tech.kayys.wayang.workflow.service.multiregion;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Region health status with metrics and diagnostics
 */
public record RegionHealthStatus(
        String regionId,
        HealthState state,
        Instant lastChecked,
        double latencyMillis,
        double errorRate,
        int activeConnections,
        Map<String, Object> metrics,
        Optional<String> diagnosticMessage) {
    public enum HealthState {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        UNKNOWN
    }

    public boolean isHealthy() {
        return state == HealthState.HEALTHY;
    }

    public boolean isOperational() {
        return state == HealthState.HEALTHY || state == HealthState.DEGRADED;
    }

    public static Builder builder(String regionId) {
        return new Builder(regionId);
    }

    public static class Builder {
        private final String regionId;
        private HealthState state = HealthState.UNKNOWN;
        private Instant lastChecked = Instant.now();
        private double latencyMillis = 0.0;
        private double errorRate = 0.0;
        private int activeConnections = 0;
        private Map<String, Object> metrics = new HashMap<>();
        private Optional<String> diagnosticMessage = Optional.empty();

        public Builder(String regionId) {
            this.regionId = regionId;
        }

        public Builder state(HealthState state) {
            this.state = state;
            return this;
        }

        public Builder lastChecked(Instant lastChecked) {
            this.lastChecked = lastChecked;
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

        public Builder diagnosticMessage(Optional<String> diagnosticMessage) {
            this.diagnosticMessage = diagnosticMessage;
            return this;
        }

        public RegionHealthStatus build() {
            return new RegionHealthStatus(
                    regionId,
                    state,
                    lastChecked,
                    latencyMillis,
                    errorRate,
                    activeConnections,
                    Collections.unmodifiableMap(metrics),
                    diagnosticMessage);
        }
    }
}
