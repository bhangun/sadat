package tech.kayys.wayang.workflow.multiregion.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.multiregion.model.RegionSelectionStrategy;

/**
 * Latency-based region selection
 */
@ApplicationScoped
public class LatencyBasedStrategy implements RegionSelectionStrategy {
    @Override
    public String selectRegion(String tenantId, List<String> healthyRegions, Optional<Map<String, Object>> context) {
        // Simplified implementation - in reality would query latency metrics
        return healthyRegions.stream()
                .findFirst()
                .orElseThrow();
    }
}
