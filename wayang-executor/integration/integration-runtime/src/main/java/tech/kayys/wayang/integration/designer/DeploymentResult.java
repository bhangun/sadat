package tech.kayys.wayang.integration.designer;

import java.time.Instant;

record DeploymentResult(
    String routeId,
    boolean success,
    String message,
    Instant deployedAt
) {}