package tech.kayys.wayang.workflow.multiregion.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.multiregion.model.RegionSelectionStrategy;

/**
 * Round-robin region selection
 */
@ApplicationScoped
public class RoundRobinStrategy implements RegionSelectionStrategy {
    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public String selectRegion(String tenantId, List<String> healthyRegions, Optional<Map<String, Object>> context) {
        if (healthyRegions.isEmpty()) {
            throw new IllegalStateException("No healthy regions available");
        }

        int index = Math.abs(counter.getAndIncrement() % healthyRegions.size());
        return healthyRegions.get(index);
    }
}
