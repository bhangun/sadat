package tech.kayys.wayang.organization.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import tech.kayys.wayang.billing.model.DomainEvent;

public record OrganizationActivatedEvent(
    String eventId,
    String tenantId,
    UUID organizationId,
    Instant occurredAt,
    Map<String, Object> metadata
) implements DomainEvent {
    @Override
    public String eventType() { return "organization.activated"; }
}