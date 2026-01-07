package tech.kayys.wayang.invoice.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import tech.kayys.wayang.billing.model.DomainEvent;

public record InvoiceGeneratedEvent(
        String eventId,
        String tenantId,
        UUID invoiceId,
        String invoiceNumber,
        java.math.BigDecimal totalAmount,
        Instant occurredAt,
        Map<String, Object> metadata) implements DomainEvent {
    @Override
    public String eventType() {
        return "invoice.generated";
    }
}
