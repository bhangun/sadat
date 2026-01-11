package tech.kayys.wayang.subscription.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import tech.kayys.wayang.billing.model.DomainEvent;

public record SubscriptionCancelledEvent(
    String eventId,
    String tenantId,
    UUID subscriptionId,
    boolean immediate,
    String reason,
    Instant occurredAt,
    Map<String, Object> metadata
) implements DomainEvent {
    @Override
    public String eventType() { return "subscription.cancelled"; }
}
