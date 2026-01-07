package tech.kayys.wayang.billing.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record QuotaExceededEvent(
    String eventId,
    String tenantId,
    UUID organizationId,
    UsageType usageType,
    long currentUsage,
    long limit,
    Instant occurredAt,
    Map<String, Object> metadata
) implements DomainEvent {
    @Override
    public String eventType() { return "quota.exceeded"; }
}
