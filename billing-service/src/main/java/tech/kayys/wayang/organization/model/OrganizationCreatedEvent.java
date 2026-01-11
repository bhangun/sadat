package tech.kayys.wayang.organization.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import tech.kayys.wayang.billing.model.DomainEvent;

/**
 * Organization events
 */
public record OrganizationCreatedEvent(
    String eventId,
    String tenantId,
    UUID organizationId,
    String name,
    String slug,
    OrganizationType orgType,
    Instant occurredAt,
    Map<String, Object> metadata
) implements DomainEvent {
    @Override
    public String eventType() { return "organization.created"; }
}