package tech.kayys.wayang.integration.designer;

import java.time.Instant;
import java.util.List;

record ValidationResult(
    String routeId,
    boolean isValid,
    List<ValidationIssue> issues,
    Instant validatedAt
) {}