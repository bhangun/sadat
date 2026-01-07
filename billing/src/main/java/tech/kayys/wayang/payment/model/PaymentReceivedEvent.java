package tech.kayys.wayang.payment.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import tech.kayys.wayang.billing.model.DomainEvent;

public record PaymentReceivedEvent(
    String eventId,
    String tenantId,
    UUID invoiceId,
    String transactionId,
    java.math.BigDecimal amount,
    Instant occurredAt,
    Map<String, Object> metadata
) implements DomainEvent {
    @Override
    public String eventType() { return "payment.received"; }
}
