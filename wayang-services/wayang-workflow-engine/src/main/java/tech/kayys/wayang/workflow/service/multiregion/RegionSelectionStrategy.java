package tech.kayys.wayang.workflow.service.multiregion;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Strategy pattern for region selection
 */
public interface RegionSelectionStrategy {
    String selectRegion(String tenantId, List<String> healthyRegions, Optional<Map<String, Object>> context);
}
