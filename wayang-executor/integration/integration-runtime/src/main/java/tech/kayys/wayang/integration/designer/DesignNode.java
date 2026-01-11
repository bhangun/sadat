package tech.kayys.wayang.integration.designer;

import java.time.Instant;
import java.util.List;
import java.util.Map;

record DesignNode(
    String nodeId,
    String nodeType,
    String label,
    Position position,
    Map<String, Object> configuration,
    List<String> inputPorts,
    List<String> outputPorts,
    Instant createdAt
) {}