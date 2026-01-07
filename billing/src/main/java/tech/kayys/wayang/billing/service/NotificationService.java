package tech.kayys.wayang.billing.service;

import java.util.UUID;
import tech.kayys.wayang.subscription.domain.Subscription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.billing.model.UsageType;
import tech.kayys.wayang.invoice.domain.Invoice;
import tech.kayys.wayang.organization.domain.Organization;

@ApplicationScoped
public class NotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);

    @Inject
    EmailService emailService;

    @Inject
    SlackService slackService;

    @Inject
    WebhookService webhookService;

    /**
     * Send welcome email
     */
    public Uni<Void> sendWelcomeEmail(UUID organizationId) {
        LOG.debug(null);
        return Organization.<Organization>findById(organizationId)
                .flatMap(org -> {
                    if (org == null || org.billingEmail == null) {
                        return Uni.createFrom().voidItem();
                    }

                    String subject = "Welcome to Silat!";
                    String body = buildWelcomeEmail(org);

                    return emailService.send(org.billingEmail, subject, body);
                });
    }

    /**
     * Send subscription confirmation
     */
    public Uni<Void> sendSubscriptionConfirmation(Subscription sub) {
        String subject = "Subscription Activated - " + sub.plan.name;
        String body = buildSubscriptionEmail(sub);

        return emailService.send(
                sub.organization.billingEmail,
                subject,
                body);
    }

    public Uni<Void> sendSubscriptionConfirmationById(UUID subscriptionId) {
        return Subscription.<Subscription>findById(subscriptionId)
                .flatMap(sub -> sub != null ? sendSubscriptionConfirmation(sub) : Uni.createFrom().voidItem());
    }

    /**
     * Send suspension notification
     */
    public Uni<Void> sendSuspensionNotification(Organization org, String reason) {
        String subject = "Account Suspended";
        String body = buildSuspensionEmail(org, reason);

        return emailService.send(org.billingEmail, subject, body)
                .flatMap(v ->
                // Also send Slack notification if configured
                slackService.sendAlert(org, "Account Suspended", reason));
    }

    /**
     * Send activation notification
     */
    public Uni<Void> sendActivationNotification(Organization org) {
        String subject = "Account Activated";
        String body = "Your Silat account has been activated.";

        return emailService.send(org.billingEmail, subject, body);
    }

    /**
     * Send cancellation confirmation
     */
    public Uni<Void> sendCancellationConfirmation(Subscription sub) {
        String subject = "Subscription Cancelled";
        String body = buildCancellationEmail(sub);

        return emailService.send(
                sub.organization.billingEmail,
                subject,
                body);
    }

    /**
     * Send quota exceeded alert
     */
    public Uni<Void> sendQuotaExceededAlert(UUID organizationId, UsageType usageType) {
        return Organization.<Organization>findById(organizationId)
                .flatMap(org -> {
                    if (org == null) {
                        return Uni.createFrom().voidItem();
                    }

                    String subject = "Quota Exceeded: " + usageType;
                    String body = buildQuotaExceededEmail(org, usageType);

                    return emailService.send(org.billingEmail, subject, body)
                            .flatMap(v -> slackService.sendAlert(
                                    org,
                                    "Quota Exceeded",
                                    usageType.toString()));
                });
    }

    /**
     * Send invoice email
     */
    public Uni<Void> sendInvoiceEmail(UUID invoiceId) {
        return Invoice.<Invoice>findById(invoiceId)
                .flatMap(invoice -> {
                    if (invoice == null) {
                        return Uni.createFrom().voidItem();
                    }

                    String subject = "Invoice " + invoice.invoiceNumber;
                    String body = buildInvoiceEmail(invoice);

                    return emailService.send(
                            invoice.organization.billingEmail,
                            subject,
                            body);
                });
    }

    /**
     * Send payment confirmation
     */
    public Uni<Void> sendPaymentConfirmation(Invoice invoice) {
        LOG.debug("Sending payment confirmation for invoice: {}", invoice.invoiceId);
        return Organization.<Organization>findById(invoice.organization.organizationId)
                .flatMap(org -> {
                    if (org == null || org.billingEmail == null) {
                        return Uni.createFrom().voidItem();
                    }

                    String subject = "Payment Confirmation - Invoice " + invoice.invoiceNumber;
                    String body = buildPaymentConfirmationEmail(invoice);

                    return emailService.send(org.billingEmail, subject, body);
                });
    }

    /**
     * Send generic email
     */
    public Uni<Void> sendEmail(String to, String subject, String body) {
        return emailService.send(to, subject, body);
    }

    // Email builders

    private String buildWelcomeEmail(Organization org) {
        return String.format("""
                Hello %s,

                Welcome to Silat! Your account has been created successfully.

                Tenant ID: %s
                Organization: %s

                Get started at: https://silat.kayys.tech

                If you have any questions, contact us at support@silat.io

                Best regards,
                The Silat Team
                """, org.name, org.tenantId, org.name);
    }

    private String buildSubscriptionEmail(Subscription sub) {
        return String.format("""
                Your subscription to %s has been activated.

                Plan: %s
                Billing Cycle: %s
                Amount: $%.2f %s
                Next billing date: %s

                View your subscription: https://silat.kayys.tech/billing

                Thank you for choosing Silat!
                """,
                sub.plan.name,
                sub.plan.name,
                sub.billingCycle,
                sub.basePrice,
                sub.currency,
                sub.currentPeriodEnd);
    }

    private String buildSuspensionEmail(Organization org, String reason) {
        return String.format("""
                Your Silat account has been suspended.

                Reason: %s

                Please contact support to resolve this issue.

                Support: support@silat.io
                """, reason);
    }

    private String buildCancellationEmail(Subscription sub) {
        return String.format("""
                Your subscription has been cancelled.

                %s

                Your service will continue until: %s

                We're sorry to see you go. If you have feedback,
                please let us know at feedback@silat.io
                """,
                sub.cancelAtPeriodEnd ? "Your subscription will be cancelled at the end of the billing period."
                        : "Your subscription has been cancelled immediately.",
                sub.currentPeriodEnd);
    }

    private String buildQuotaExceededEmail(Organization org, UsageType usageType) {
        return String.format("""
                Your usage quota has been exceeded.

                Resource: %s

                Please upgrade your plan to continue service.

                Manage your subscription: https://silat.kayys.tech/billing
                """, usageType);
    }

    private String buildInvoiceEmail(Invoice invoice) {
        return String.format("""
                Dear %s,

                A new invoice has been generated for your account.

                Invoice Details:
                - Invoice Number: %s
                - Amount Due: %s %s
                - Due Date: %s
                - Period: %s to %s

                You can view and pay this invoice at: https://silat.kayys.tech/invoices/%s

                If you have any questions, please contact our billing team.

                Thank you for your business!

                Best regards,
                Silat Billing Team
                https://silat.kayys.tech
                """,
                invoice.organization.name,
                invoice.invoiceNumber,
                invoice.amountDue,
                invoice.currency,
                invoice.dueDate,
                invoice.periodStart,
                invoice.periodEnd,
                invoice.invoiceId);
    }

    private String buildPaymentConfirmationEmail(Invoice invoice) {
        return String.format("""
                Dear %s,

                Your payment for Invoice %s has been successfully processed.

                Payment Details:
                - Invoice Number: %s
                - Amount Paid: %s %s
                - Payment Date: %s
                - Transaction ID: %s

                A receipt has been attached to this email for your records.

                Thank you for your prompt payment!

                Best regards,
                Silat Billing Team
                https://silat.kayys.tech
                """,
                invoice.organization.name,
                invoice.invoiceNumber,
                invoice.invoiceNumber,
                invoice.amountDue,
                invoice.currency,
                invoice.paidAt != null ? invoice.paidAt.toString() : Instant.now().toString(),
                invoice.paymentTransactionId);
    }

    /**
     * Send churn alert
     */
    public Uni<Void> sendChurnAlert(tech.kayys.wayang.billing.domain.ChurnPrediction prediction) {
        return Organization.<Organization>findById(prediction.organization.organizationId)
                .flatMap(org -> {
                    if (org == null) {
                        return Uni.createFrom().voidItem();
                    }

                    String subject = "High Churn Risk Alert - " + org.name;
                    String body = String.format("""
                            High churn risk detected for %s.

                            Risk Level: %s
                            Churn Probability: %.2f%%
                            Predicted Date: %s

                            Please take immediate action.
                            """,
                            org.name,
                            prediction.riskLevel,
                            prediction.churnProbability * 100,
                            prediction.predictedChurnDate);

                    return emailService.send(org.billingEmail, subject, body);
                });
    }
}
