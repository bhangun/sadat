package tech.kayys.wayang.workflow.kernel;

import java.util.HashMap;
import java.util.Map;

import io.smallrye.mutiny.Uni;

/**
 * Lifecycle management for workflow components
 */
public interface ManagedWorkflowComponent {

    /**
     * Initialize the component with configuration
     */
    Uni<Void> initialize(ComponentConfig config);

    /**
     * Start the component (ready to process requests)
     */
    Uni<Void> start();

    /**
     * Stop the component gracefully
     */
    Uni<Void> stop();

    /**
     * Check component health
     */
    Uni<HealthStatus> healthCheck();

    /**
     * Get component metrics
     */
    Uni<ComponentMetrics> getMetrics();

    public static class ComponentConfig {
        private final String componentName;
        private final Map<String, Object> properties;

        public ComponentConfig(String componentName, Map<String, Object> properties) {
            this.componentName = componentName;
            this.properties = properties != null ? new HashMap<>(properties) : new HashMap<>();
        }

        // Getters and builder...
    }

    public enum HealthStatus {
        HEALTHY, DEGRADED, UNHEALTHY
    }

    public record ComponentMetrics(
            long totalRequests,
            long successfulRequests,
            long failedRequests,
            double averageLatencyMs,
            Map<String, Object> customMetrics) {
    }
}
