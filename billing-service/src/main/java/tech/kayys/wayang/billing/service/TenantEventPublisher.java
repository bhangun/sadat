package tech.kayys.wayang.billing.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.billing.model.DomainEvent;
import tech.kayys.wayang.billing.model.QuotaExceededEvent;
import tech.kayys.wayang.billing.model.Record;
import tech.kayys.wayang.billing.model.UsageType;
import tech.kayys.wayang.invoice.domain.Invoice;
import tech.kayys.wayang.invoice.model.InvoiceGeneratedEvent;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.organization.model.OrganizationActivatedEvent;
import tech.kayys.wayang.organization.model.OrganizationCreatedEvent;
import tech.kayys.wayang.organization.model.OrganizationSuspendedEvent;
import tech.kayys.wayang.subscription.domain.Subscription;
import tech.kayys.wayang.subscription.model.SubscriptionCancelledEvent;
import tech.kayys.wayang.subscription.model.SubscriptionCreatedEvent;

@ApplicationScoped
public class TenantEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(TenantEventPublisher.class);

    @Inject
    @Channel("tenant-events")
    Emitter<Record<String, DomainEvent>> eventEmitter;

    /**
     * Publish organization created event
     */
    public Uni<Void> publishOrganizationCreated(Organization org) {
        OrganizationCreatedEvent event = new OrganizationCreatedEvent(
                UUID.randomUUID().toString(),
                org.tenantId,
                org.organizationId,
                org.name,
                org.slug,
                org.orgType,
                Instant.now(),
                new HashMap<>());

        return publishEvent(event);
    }

    /**
     * Publish organization suspended event
     */
    public Uni<Void> publishOrganizationSuspended(Organization org, String reason) {
        OrganizationSuspendedEvent event = new OrganizationSuspendedEvent(
                UUID.randomUUID().toString(),
                org.tenantId,
                org.organizationId,
                reason,
                Instant.now(),
                new HashMap<>());

        return publishEvent(event);
    }

    /**
     * Publish organization activated event
     */
    public Uni<Void> publishOrganizationActivated(Organization org) {
        OrganizationActivatedEvent event = new OrganizationActivatedEvent(
                UUID.randomUUID().toString(),
                org.tenantId,
                org.organizationId,
                Instant.now(),
                new HashMap<>());

        return publishEvent(event);
    }

    /**
     * Publish organization deleted event
     */
    public Uni<Void> publishOrganizationDeleted(Organization org) {
        // Similar implementation
        return Uni.createFrom().voidItem();
    }

    /**
     * Publish subscription created event
     */
    public Uni<Void> publishSubscriptionCreated(Subscription sub) {
        SubscriptionCreatedEvent event = new SubscriptionCreatedEvent(
                UUID.randomUUID().toString(),
                sub.organization.tenantId,
                sub.subscriptionId,
                sub.organization.organizationId,
                sub.plan.planId,
                sub.plan.name,
                Instant.now(),
                new HashMap<>());

        return publishEvent(event);
    }

    /**
     * Publish subscription updated event
     */
    public Uni<Void> publishSubscriptionUpdated(Subscription sub) {
        // Implementation
        return Uni.createFrom().voidItem();
    }

    /**
     * Publish subscription renewed event
     */
    public Uni<Void> publishSubscriptionRenewed(Subscription sub) {
        // Placeholder event for now
        return Uni.createFrom().voidItem();
    }

    /**
     * Publish subscription cancelled event
     */
    public Uni<Void> publishSubscriptionCancelled(Subscription sub, boolean immediate) {
        SubscriptionCancelledEvent event = new SubscriptionCancelledEvent(
                UUID.randomUUID().toString(),
                sub.organization.tenantId,
                sub.subscriptionId,
                immediate,
                sub.cancellationReason,
                Instant.now(),
                new HashMap<>());

        return publishEvent(event);
    }

    /**
     * Publish quota exceeded event
     */
    public Uni<Void> publishQuotaExceeded(Organization org, UsageType usageType) {
        QuotaExceededEvent event = new QuotaExceededEvent(
                UUID.randomUUID().toString(),
                org.tenantId,
                org.organizationId,
                usageType,
                0L, // Will be filled by quota service
                0L,
                Instant.now(),
                new HashMap<>());

        return publishEvent(event);
    }

    /**
     * Publish invoice generated event
     */
    public Uni<Void> publishInvoiceGenerated(Invoice invoice) {
        InvoiceGeneratedEvent event = new InvoiceGeneratedEvent(
                UUID.randomUUID().toString(),
                invoice.organization.tenantId,
                invoice.invoiceId,
                invoice.invoiceNumber,
                invoice.totalAmount,
                Instant.now(),
                new HashMap<>());

        return publishEvent(event);
    }

    /**
     * Generic event publisher
     */
    private Uni<Void> publishEvent(DomainEvent event) {
        LOG.info("Publishing event: {} for tenant: {}",
                event.eventType(), event.tenantId());

        return Uni.createFrom().completionStage(
                eventEmitter.send(Record.of(event.tenantId(), event))
                        .toCompletableFuture())
                .replaceWithVoid()
                .onFailure().invoke(error -> LOG.error("Failed to publish event: {}", event.eventType(), error));
    }
}