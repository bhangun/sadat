package tech.kayys.wayang.dto;

import java.time.Instant;

public record TenantMetrics(
        String tenantId,
        long activeWorkflows,
        long activeAgents,
        long executionsToday,
        double costToday,
        Instant timestamp) {
}
