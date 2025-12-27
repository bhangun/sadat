package tech.kayys.wayang.workflow.multiregion.model;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.workflow.multiregion.dto.HealthCheckResponse;

/**
 * Interface for health check strategies
 */
public interface HealthCheckStrategy {
    Uni<HealthCheckResponse> checkRegionHealth(String regionId);
}
