package tech.kayys.wayang.integration.designer;

import java.time.Instant;

record GeneratedRoute(
    String routeId,
    String camelDSL,
    String javaCode,
    String routeName,
    Instant generatedAt
) {}