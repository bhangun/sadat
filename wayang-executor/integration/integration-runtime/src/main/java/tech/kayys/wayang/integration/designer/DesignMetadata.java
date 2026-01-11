package tech.kayys.wayang.integration.designer;

import java.time.Instant;
import java.util.Map;

record DesignMetadata(
    Instant createdAt,
    Instant updatedAt,
    String version,
    String status,
    Map<String, Object> tags
) {}