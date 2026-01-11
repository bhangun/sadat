package tech.kayys.wayang.integration.designer;

import java.util.Map;

record CreateRouteRequest(
    String name,
    String description,
    String category,
    String tenantId
) {}