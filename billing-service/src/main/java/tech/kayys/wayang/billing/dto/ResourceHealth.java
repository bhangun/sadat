package tech.kayys.wayang.billing.dto;

import java.time.Instant;

public record ResourceHealth(
    String resourceId,
    String status,
    Instant lastCheck,
    double utilizationPercent
) {}