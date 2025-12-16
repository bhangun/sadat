package tech.kayys.wayang.plugin.node;

import java.util.Optional;

// Minimal HealthStatus so the interface compiles when the referenced type is not provided elsewhere.
final class HealthStatus {
    private final boolean healthy;
    private final String message;

    private HealthStatus(boolean healthy, String message) {
        this.healthy = healthy;
        this.message = message;
    }

    public static HealthStatus healthy() {
        return new HealthStatus(true, null);
    }

    public static HealthStatus unhealthy(String message) {
        return new HealthStatus(false, message);
    }

    public boolean isHealthy() {
        return this.healthy;
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(this.message);
    }

    @Override
    public String toString() {
        return "HealthStatus[healthy=" + healthy + (message != null ? ", message=" + message : "") + "]";
    }
}