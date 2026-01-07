package tech.kayys.wayang.payment.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import tech.kayys.wayang.billing.model.DomainEvent;

public record PaymentFailedEvent(
    String eventId,
    String tenantId,
    UUID invoiceId,
    String reason,
    int attemptCount,
    Instant occurredAt,
    Map<String, Object> metadata
) implements DomainEvent {
    @Override
    public String eventType() { return "payment.failed"; }
}
