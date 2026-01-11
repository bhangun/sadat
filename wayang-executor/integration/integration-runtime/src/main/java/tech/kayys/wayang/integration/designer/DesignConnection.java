package tech.kayys.wayang.integration.designer;

import java.time.Instant;

record DesignConnection(
    String connectionId,
    String sourceNodeId,
    String targetNodeId,
    String connectionType,
    String condition,
    Instant createdAt
) {}