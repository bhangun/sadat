package tech.kayys.wayang.billing.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.invoice.domain.Invoice;

@ApplicationScoped
public class DunningManager {

    @Inject
    NotificationService notificationService;

    public Uni<Void> handleFailedPayment(Invoice invoice) {
        // Calculate days past due
        long daysPastDue = ChronoUnit.DAYS.between(invoice.dueDate, Instant.now());

        if (daysPastDue <= 0) {
            // Not past due yet
            return Uni.createFrom().voidItem();
        }

        // Determine dunning level based on days past due
        DunningLevel level = getDunningLevel(daysPastDue);

        // Send appropriate notification
        return switch (level) {
            case REMINDER -> sendReminderEmail(invoice);
            case WARNING -> sendWarningEmail(invoice);
            case FINAL_NOTICE -> sendFinalNoticeEmail(invoice);
            case COLLECTION -> initiateCollectionProcess(invoice);
        };
    }

    private DunningLevel getDunningLevel(long daysPastDue) {
        if (daysPastDue <= 7)
            return DunningLevel.REMINDER;
        if (daysPastDue <= 14)
            return DunningLevel.WARNING;
        if (daysPastDue <= 30)
            return DunningLevel.FINAL_NOTICE;
        return DunningLevel.COLLECTION;
    }

    private Uni<Void> sendReminderEmail(Invoice invoice) {
        return notificationService.sendEmail(
                invoice.organization.billingEmail,
                "Payment Reminder - Invoice " + invoice.invoiceNumber,
                buildReminderEmail(invoice));
    }

    private Uni<Void> sendWarningEmail(Invoice invoice) {
        return notificationService.sendEmail(
                invoice.organization.billingEmail,
                "URGENT: Payment Overdue - Invoice " + invoice.invoiceNumber,
                buildWarningEmail(invoice));
    }

    private Uni<Void> sendFinalNoticeEmail(Invoice invoice) {
        return notificationService.sendEmail(
                invoice.organization.billingEmail,
                "FINAL NOTICE: Payment Required - Invoice " + invoice.invoiceNumber,
                buildFinalNoticeEmail(invoice));
    }

    private Uni<Void> initiateCollectionProcess(Invoice invoice) {
        // In a real implementation, this would integrate with a collections agency
        // For now, just send a final email
        return notificationService.sendEmail(
                invoice.organization.billingEmail,
                "COLLECTION NOTICE - Invoice " + invoice.invoiceNumber,
                buildCollectionNoticeEmail(invoice));
    }

    private String buildReminderEmail(Invoice invoice) {
        return String.format("""
                Dear %s,

                This is a friendly reminder that payment for Invoice %s is due.

                Invoice Details:
                - Invoice Number: %s
                - Amount Due: %s %s
                - Due Date: %s

                Please ensure payment is made by the due date to avoid any service interruptions.

                Best regards,
                Silat Billing Team
                """,
                invoice.organization.name,
                invoice.invoiceNumber,
                invoice.invoiceNumber,
                invoice.amountDue,
                invoice.currency,
                invoice.dueDate.toString());
    }

    private String buildWarningEmail(Invoice invoice) {
        long daysOverdue = ChronoUnit.DAYS.between(invoice.dueDate, Instant.now());
        return String.format("""
                Dear %s,

                URGENT: Your payment for Invoice %s is %d days overdue.

                Invoice Details:
                - Invoice Number: %s
                - Amount Due: %s %s
                - Due Date: %s
                - Days Overdue: %d

                Immediate payment is required to avoid service suspension.

                Best regards,
                Silat Billing Team
                """,
                invoice.organization.name,
                invoice.invoiceNumber,
                daysOverdue,
                invoice.invoiceNumber,
                invoice.amountDue,
                invoice.currency,
                invoice.dueDate.toString(),
                daysOverdue);
    }

    private String buildFinalNoticeEmail(Invoice invoice) {
        return String.format("""
                Dear %s,

                FINAL NOTICE: Payment for Invoice %s is now overdue.

                Invoice Details:
                - Invoice Number: %s
                - Amount Due: %s %s
                - Due Date: %s

                This is your final notice before collection proceedings begin.

                Best regards,
                Silat Billing Team
                """,
                invoice.organization.name,
                invoice.invoiceNumber,
                invoice.invoiceNumber,
                invoice.amountDue,
                invoice.currency,
                invoice.dueDate.toString());
    }

    private String buildCollectionNoticeEmail(Invoice invoice) {
        return String.format("""
                Dear %s,

                COLLECTION NOTICE: Invoice %s has been referred to collections.

                Invoice Details:
                - Invoice Number: %s
                - Amount Due: %s %s
                - Due Date: %s

                Please contact us immediately to resolve this matter.

                Best regards,
                Silat Billing Team
                """,
                invoice.organization.name,
                invoice.invoiceNumber,
                invoice.invoiceNumber,
                invoice.amountDue,
                invoice.currency,
                invoice.dueDate.toString());
    }

    private enum DunningLevel {
        REMINDER, WARNING, FINAL_NOTICE, COLLECTION
    }
}