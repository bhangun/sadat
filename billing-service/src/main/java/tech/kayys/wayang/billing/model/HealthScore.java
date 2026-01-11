package tech.kayys.wayang.billing.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record HealthScore(
    UUID organizationId,
    double overallScore,
    HealthStatus status,
    Instant calculatedAt,
    Map<String, Double> componentScores
) {}

