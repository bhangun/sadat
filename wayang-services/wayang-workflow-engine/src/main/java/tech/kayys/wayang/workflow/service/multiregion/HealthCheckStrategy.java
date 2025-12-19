package tech.kayys.wayang.workflow.service.multiregion;

import io.smallrye.mutiny.Uni;

/**
 * Interface for health check strategies
 */
public interface HealthCheckStrategy {
    Uni<HealthCheckResponse> checkRegionHealth(String regionId);
}
