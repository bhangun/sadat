package tech.kayys.wayang.organization.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.mutiny.core.eventbus.Message;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.billing.model.DomainEvent;
import tech.kayys.wayang.billing.model.QuotaExceededEvent;
import tech.kayys.wayang.billing.service.AuditLogger;
import tech.kayys.wayang.billing.service.NotificationService;
import tech.kayys.wayang.billing.service.WebSocketNotifier;
import tech.kayys.wayang.invoice.model.InvoiceGeneratedEvent;
import tech.kayys.wayang.organization.model.OrganizationCreatedEvent;
import tech.kayys.wayang.organization.model.OrganizationSuspendedEvent;
import tech.kayys.wayang.subscription.model.SubscriptionCreatedEvent;

/**
 * Organization event listener
 */
@ApplicationScoped
public class OrganizationEventListener {

    private static final Logger LOG = LoggerFactory.getLogger(OrganizationEventListener.class);

    @Inject
    NotificationService notificationService;

    @Inject
    AuditLogger auditLogger;

    @Inject
    WebSocketNotifier webSocketNotifier;

    @Incoming("tenant-events")
    public CompletableFuture<Void> onEvent(Message<DomainEvent> message) {
        DomainEvent event = message.getPayload();

        LOG.debug("Received event: {} for tenant: {}",
                event.eventType(), event.tenantId());

        CompletableFuture<Void> future = switch (event.eventType()) {
            case "organization.created" ->
                handleOrganizationCreated((OrganizationCreatedEvent) event);
            case "organization.suspended" ->
                handleOrganizationSuspended((OrganizationSuspendedEvent) event);
            case "subscription.created" ->
                handleSubscriptionCreated((SubscriptionCreatedEvent) event);
            case "quota.exceeded" ->
                handleQuotaExceeded((QuotaExceededEvent) event);
            case "invoice.generated" ->
                handleInvoiceGenerated((InvoiceGeneratedEvent) event);
            default -> CompletableFuture.completedFuture(null);
        };

        return future.thenRun(() -> message.ack());
    }

    private CompletableFuture<Void> handleOrganizationCreated(
            OrganizationCreatedEvent event) {

        LOG.info("Handling organization created: {}", event.organizationId());

        return notificationService.sendWelcomeEmail(
                event.organizationId()).subscribeAsCompletionStage();
    }

    private CompletableFuture<Void> handleOrganizationSuspended(
            OrganizationSuspendedEvent event) {

        LOG.warn("Handling organization suspended: {}", event.organizationId());

        return CompletableFuture.allOf(
                webSocketNotifier.notifyOrganization(
                        event.tenantId(),
                        "organization.suspended",
                        Map.of("reason", event.reason())).subscribeAsCompletionStage(),

                auditLogger.logEvent(
                        event.tenantId(),
                        "ORGANIZATION_SUSPENDED",
                        event.reason()).subscribeAsCompletionStage());
    }

    private CompletableFuture<Void> handleSubscriptionCreated(
            SubscriptionCreatedEvent event) {

        LOG.info("Handling subscription created: {}", event.subscriptionId());

        return notificationService.sendSubscriptionConfirmationById(
                event.subscriptionId()).subscribeAsCompletionStage();
    }

    private CompletableFuture<Void> handleQuotaExceeded(
            QuotaExceededEvent event) {

        LOG.error("Handling quota exceeded: {} for org: {}",
                event.usageType(), event.organizationId());

        return notificationService.sendQuotaExceededAlert(
                event.organizationId(),
                event.usageType()).subscribeAsCompletionStage();
    }

    private CompletableFuture<Void> handleInvoiceGenerated(
            InvoiceGeneratedEvent event) {

        LOG.info("Handling invoice generated: {}", event.invoiceNumber());

        return notificationService.sendInvoiceEmail(
                event.invoiceId()).subscribeAsCompletionStage();
    }
}
