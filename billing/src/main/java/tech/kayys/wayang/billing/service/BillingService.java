package tech.kayys.wayang.billing.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.billing.domain.UsageAggregate;
import tech.kayys.wayang.billing.dto.BillingSummary;
import tech.kayys.wayang.invoice.domain.Invoice;
import tech.kayys.wayang.invoice.model.InvoiceStatus;
import tech.kayys.wayang.invoice.service.InvoiceGenerator;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.payment.dto.PaymentResult;
import tech.kayys.wayang.payment.service.PaymentProcessor;
import tech.kayys.wayang.subscription.domain.Subscription;
import tech.kayys.wayang.subscription.model.SubscriptionStatus;

@ApplicationScoped
public class BillingService {

    private static final Logger LOG = LoggerFactory.getLogger(BillingService.class);

    @Inject
    InvoiceGenerator invoiceGenerator;

    @Inject
    PaymentProcessor paymentProcessor;

    @Inject
    DunningManager dunningManager;

    @Inject
    TaxCalculator taxCalculator;

    @Inject
    NotificationService notificationService;

    /**
     * Generate invoice for organization
     */
    public Uni<Invoice> generateInvoice(Organization organization, Instant periodEnd) {
        LOG.info("Generating invoice for org: {} period: {}",
                organization.tenantId, periodEnd);

        return invoiceGenerator.generate(organization, periodEnd)
                .flatMap(invoice -> {
                    // Calculate tax
                    return taxCalculator.calculateTax(invoice)
                            .map(taxRate -> {
                                invoice.applyTax(taxRate);
                                return invoice;
                            });
                })
                .flatMap(this::persistInvoice)
                .flatMap(invoice -> {
                    invoice.finalize();
                    return persistInvoice(invoice);
                })
                .flatMap(invoice -> {
                    // Send invoice email
                    return notificationService.sendInvoiceEmail(invoice.invoiceId)
                            .replaceWith(invoice);
                })
                .invoke(invoice -> LOG.info("Invoice generated: {} amount: ${}",
                        invoice.invoiceNumber, invoice.totalAmount));
    }

    /**
     * Process payment for invoice
     */
    public Uni<PaymentResult> processPayment(Invoice invoice) {
        LOG.info("Processing payment for invoice: {}", invoice.invoiceNumber);

        if (invoice.status == InvoiceStatus.PAID) {
            return Uni.createFrom().item(
                    new PaymentResult(true, "Already paid", invoice.paymentTransactionId));
        }

        return paymentProcessor.charge(
                invoice.organization,
                invoice.amountDue,
                invoice.currency,
                invoice.paymentMethodId,
                Map.of("invoice", invoice.invoiceNumber))
                .flatMap(result -> {
                    if (result.success()) {
                        invoice.markPaid(result.transactionId(), invoice.amountDue);
                        return persistInvoice(invoice)
                                .flatMap(inv -> notificationService.sendPaymentConfirmation(inv)
                                        .replaceWith(result));
                    } else {
                        // Payment failed
                        invoice.attemptCount++;
                        invoice.lastAttemptAt = Instant.now();

                        return persistInvoice(invoice)
                                .flatMap(inv -> dunningManager.handleFailedPayment(inv))
                                .replaceWith(result);
                    }
                });
    }

    /**
     * Create credit note
     */
    public Uni<Invoice> createCreditNote(
            Invoice originalInvoice,
            BigDecimal amount,
            String reason) {

        LOG.info("Creating credit note for invoice: {} amount: ${}",
                originalInvoice.invoiceNumber, amount);

        return Panache.withTransaction(() -> {
            Invoice creditNote = new Invoice();
            creditNote.invoiceNumber = generateCreditNoteNumber();
            creditNote.organization = originalInvoice.organization;
            creditNote.subscription = originalInvoice.subscription;
            creditNote.status = InvoiceStatus.PAID;
            creditNote.invoiceDate = Instant.now();
            creditNote.periodStart = originalInvoice.periodStart;
            creditNote.periodEnd = originalInvoice.periodEnd;
            creditNote.totalAmount = amount.negate();
            creditNote.amountDue = BigDecimal.ZERO;
            creditNote.currency = originalInvoice.currency;
            creditNote.notes = "Credit note for invoice " +
                    originalInvoice.invoiceNumber + ": " + reason;
            creditNote.createdAt = Instant.now();
            creditNote.updatedAt = Instant.now();

            return creditNote.persist()
                    .map(v -> creditNote);
        });
    }

    /**
     * Get billing summary
     */
    public Uni<BillingSummary> getBillingSummary(Organization organization) {
        return Uni.combine().all()
                .unis(
                        getOutstandingBalance(organization),
                        getCurrentPeriodUsage(organization),
                        getUpcomingInvoiceEstimate(organization),
                        getPaymentHistory(organization))
                .asTuple()
                .map(tuple -> new BillingSummary(
                        organization.tenantId,
                        tuple.getItem1(),
                        tuple.getItem2(),
                        tuple.getItem3(),
                        tuple.getItem4()));
    }

    /**
     * Scheduled invoice generation
     */
    @Scheduled(cron = "0 0 0 * * ?") // Daily at midnight
    public void generateDueInvoices() {
        LOG.info("Checking for organizations needing invoices");

        Instant now = Instant.now();

        Subscription.<Subscription>find(
                "status = ?1 and currentPeriodEnd <= ?2 and currentPeriodEnd > ?3",
                SubscriptionStatus.ACTIVE,
                now.plus(1, ChronoUnit.DAYS),
                now).list()
                .subscribe().with(
                        subscriptions -> {
                            LOG.info("Generating invoices for {} subscriptions", subscriptions.size());
                            subscriptions.forEach(sub -> generateInvoice(sub.organization, sub.currentPeriodEnd)
                                    .subscribe().with(
                                            invoice -> LOG.info("Invoice generated: {}", invoice.invoiceNumber),
                                            error -> LOG.error("Failed to generate invoice", error)));
                        },
                        error -> LOG.error("Error in invoice generation", error));
    }

    /**
     * Scheduled payment collection
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    public void collectPayments() {
        LOG.info("Collecting payments for open invoices");

        Invoice.<Invoice>find(
                "status = ?1 and dueDate <= ?2",
                InvoiceStatus.OPEN,
                Instant.now()).list()
                .subscribe().with(
                        invoices -> {
                            LOG.info("Processing {} invoices", invoices.size());
                            invoices.forEach(invoice -> processPayment(invoice)
                                    .subscribe().with(
                                            result -> LOG.info("Payment processed: {} - {}",
                                                    invoice.invoiceNumber, result.success()),
                                            error -> LOG.error("Payment processing failed", error)));
                        },
                        error -> LOG.error("Error collecting payments", error));
    }

    // Helper methods

    private Uni<BigDecimal> getOutstandingBalance(Organization org) {
        return Invoice.<Invoice>find(
                "organization = ?1 and status in (?2, ?3)",
                org,
                InvoiceStatus.OPEN,
                InvoiceStatus.PARTIALLY_PAID).list()
                .map(invoices -> invoices.stream()
                        .map(inv -> inv.amountDue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private Uni<BigDecimal> getCurrentPeriodUsage(Organization org) {
        return UsageAggregate.<UsageAggregate>find(
                "organization = ?1 and yearMonth = ?2",
                org,
                YearMonth.now()).firstResult()
                .map(agg -> agg != null ? agg.totalCost : BigDecimal.ZERO);
    }

    private Uni<BigDecimal> getUpcomingInvoiceEstimate(Organization org) {
        if (org.activeSubscription == null) {
            return Uni.createFrom().item(BigDecimal.ZERO);
        }

        return getCurrentPeriodUsage(org)
                .map(usage -> {
                    BigDecimal subscriptionFee = org.activeSubscription.calculateTotalPrice();
                    return subscriptionFee.add(usage);
                });
    }

    private Uni<List<Invoice>> getPaymentHistory(Organization org) {
        return Invoice.<Invoice>find(
                "organization = ?1 order by invoiceDate desc",
                org).page(0, 12).list();
    }

    private Uni<Invoice> persistInvoice(Invoice invoice) {
        return Panache.withTransaction(() -> invoice.persist().map(v -> invoice));
    }

    private String generateCreditNoteNumber() {
        return "CN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
