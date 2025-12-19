package tech.kayys.wayang.workflow.service.multiregion;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Client for performing health checks
 */
@ApplicationScoped
public class HealthCheckClient {

    @Inject
    @RestClient
    RegionHealthEndpoint healthEndpoint;

    @ConfigProperty(name = "app.regions.health.endpoint.path", defaultValue = "/health")
    String healthEndpointPath;

    public Uni<HealthCheckResponse> checkHealth(String regionId) {
        // Implementation would make HTTP call to region's health endpoint
        // For now, return mock response
        return Uni.createFrom().item(() -> HealthCheckResponse.builder()
                .success(true)
                .latencyMillis(150.0)
                .errorRate(0.01)
                .activeConnections(100)
                .addMetric("cpu_usage", 0.45)
                .addMetric("memory_usage", 0.68)
                .diagnosticMessage("Region is healthy")
                .build());
    }
}
