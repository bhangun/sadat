package tech.kayys.wayang.integration.designer;

import java.util.Map;

record AddNodeRequest(
    String nodeType,
    String label,
    Position position,
    Map<String, Object> configuration
) {}