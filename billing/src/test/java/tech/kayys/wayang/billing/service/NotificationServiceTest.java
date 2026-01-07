package tech.kayys.wayang.billing.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tech.kayys.wayang.invoice.domain.Invoice;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.subscription.domain.Subscription;
import tech.kayys.wayang.subscription.domain.SubscriptionPlan;
import jakarta.inject.Inject;

@io.quarkus.test.junit.QuarkusTest
class NotificationServiceTest {

    @Inject
    NotificationService notificationService;

    @io.quarkus.test.InjectMock
    EmailService emailService;

    @io.quarkus.test.InjectMock
    SlackService slackService;

    private Organization organization;
    private Subscription subscription;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        // Setup mocks
        org.mockito.Mockito
                .when(emailService.send(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(io.smallrye.mutiny.Uni.createFrom().voidItem());

        org.mockito.Mockito
                .when(slackService.sendAlert(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()))
                .thenReturn(io.smallrye.mutiny.Uni.createFrom().voidItem());

        // Setup test organization
        organization = new Organization();
        organization.organizationId = UUID.randomUUID();
        organization.tenantId = "test-org-notif";
        organization.slug = "test-org-notif";
        organization.name = "Test Organization";
        organization.billingEmail = "billing@test.com";

        // Setup test subscription
        subscription = new Subscription();
        subscription.subscriptionId = UUID.randomUUID();
        subscription.organization = organization;
        subscription.plan = new SubscriptionPlan();
        subscription.plan.name = "Test Plan";
        subscription.billingCycle = tech.kayys.wayang.billing.model.BillingCycle.MONTHLY;
        subscription.basePrice = BigDecimal.valueOf(99.99);
        subscription.currency = "USD";
        subscription.currentPeriodEnd = Instant.now().plusSeconds(86400 * 30);

        // Setup test invoice
        invoice = new Invoice();
        invoice.invoiceId = UUID.randomUUID();
        invoice.invoiceNumber = "INV-001";
        invoice.organization = organization;
        invoice.amountDue = BigDecimal.valueOf(149.99);
        invoice.currency = "USD";
        invoice.dueDate = Instant.now().plusSeconds(2592000);
    }

    @Test
    @io.quarkus.test.TestTransaction
    void testSendWelcomeEmail_ValidOrganization_SendsEmail() {
        // Given
        organization.persist().await().indefinitely();

        // When
        var result = notificationService.sendWelcomeEmail(organization.organizationId).await().indefinitely();

        // Then
        // result is void, verify mock interaction
        org.mockito.Mockito.verify(emailService).send(
                org.mockito.ArgumentMatchers.eq("billing@test.com"),
                org.mockito.ArgumentMatchers.contains("Welcome"),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @io.quarkus.test.TestTransaction
    void testSendSubscriptionConfirmation_ValidSubscription_SendsEmail() {
        // When - pass object directly (no persistence needed if method uses object
        // properties)
        // Wait, NotificationService uses object properties directly.
        // It does NOT lookup subscription by ID.
        var result = notificationService.sendSubscriptionConfirmation(subscription).await().indefinitely();

        // Then
        org.mockito.Mockito.verify(emailService).send(
                org.mockito.ArgumentMatchers.eq("billing@test.com"),
                org.mockito.ArgumentMatchers.contains("Subscription Activated"),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @io.quarkus.test.TestTransaction
    void testSendInvoiceEmail_ValidInvoice_SendsEmail() {
        // Given
        organization.persist().await().indefinitely();
        invoice.persist().await().indefinitely();

        // When
        var result = notificationService.sendInvoiceEmail(invoice.invoiceId).await().indefinitely();

        // Then
        org.mockito.Mockito.verify(emailService).send(
                org.mockito.ArgumentMatchers.eq("billing@test.com"),
                org.mockito.ArgumentMatchers.contains("Invoice"),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @io.quarkus.test.TestTransaction
    void testSendPaymentConfirmation_ValidInvoice_SendsEmail() {
        // Given
        organization.persist().await().indefinitely();
        invoice.paidAt = Instant.now();
        invoice.persist().await().indefinitely();

        // When
        var result = notificationService.sendPaymentConfirmation(invoice).await().indefinitely();

        // Then
        org.mockito.Mockito.verify(emailService).send(
                org.mockito.ArgumentMatchers.eq("billing@test.com"),
                org.mockito.ArgumentMatchers.contains("Payment Confirmation"),
                org.mockito.ArgumentMatchers.anyString());
    }
}