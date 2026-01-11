package tech.kayys.wayang.invoice.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.billing.domain.UsageAggregate;
import tech.kayys.wayang.billing.model.LineItemType;
import tech.kayys.wayang.invoice.domain.Invoice;
import tech.kayys.wayang.invoice.domain.InvoiceLineItem;
import tech.kayys.wayang.invoice.model.InvoiceStatus;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.subscription.domain.Subscription;
import tech.kayys.wayang.subscription.domain.SubscriptionAddon;

/**
 * Invoice generator
 */
@ApplicationScoped
public class InvoiceGenerator {

    public Uni<Invoice> generate(Organization org, Instant periodEnd) {
        if (org.activeSubscription == null) {
            return Uni.createFrom().failure(
                    new IllegalStateException("No active subscription"));
        }

        Subscription subscription = org.activeSubscription;
        Instant periodStart = subscription.currentPeriodStart;

        return Uni.createFrom().item(() -> {
            Invoice invoice = new Invoice();
            invoice.invoiceNumber = generateInvoiceNumber();
            invoice.organization = org;
            invoice.subscription = subscription;
            invoice.status = InvoiceStatus.DRAFT;
            invoice.periodStart = periodStart;
            invoice.periodEnd = periodEnd;
            invoice.dueDate = periodEnd.plus(14, ChronoUnit.DAYS);
            invoice.currency = subscription.currency;
            invoice.paymentMethodId = subscription.paymentMethodId;
            invoice.createdAt = Instant.now();
            invoice.updatedAt = Instant.now();

            // Add subscription line item
            addSubscriptionLineItem(invoice, subscription);

            return invoice;
        })
                .flatMap(invoice ->
                // Add usage line items
                addUsageLineItems(invoice, org, periodStart, periodEnd));
    }

    private void addSubscriptionLineItem(Invoice invoice, Subscription subscription) {
        InvoiceLineItem item = new InvoiceLineItem();
        item.itemType = LineItemType.SUBSCRIPTION;
        item.description = subscription.plan.name + " - " + subscription.billingCycle;
        item.quantity = BigDecimal.ONE;
        item.unitPrice = subscription.basePrice;
        item.amount = subscription.basePrice;
        item.periodStart = invoice.periodStart;
        item.periodEnd = invoice.periodEnd;
        item.subscriptionId = subscription.subscriptionId;

        invoice.addLineItem(item);

        // Add addons
        for (SubscriptionAddon addon : subscription.addons) {
            if (addon.isActive) {
                InvoiceLineItem addonItem = new InvoiceLineItem();
                addonItem.itemType = LineItemType.ADDON;
                addonItem.description = addon.addonCatalog.name;
                addonItem.quantity = BigDecimal.valueOf(addon.quantity);
                addonItem.unitPrice = addon.price;
                addonItem.amount = addon.price.multiply(
                        BigDecimal.valueOf(addon.quantity));
                addonItem.periodStart = invoice.periodStart;
                addonItem.periodEnd = invoice.periodEnd;

                invoice.addLineItem(addonItem);
            }
        }
    }

    private Uni<Invoice> addUsageLineItems(
            Invoice invoice,
            Organization org,
            Instant periodStart,
            Instant periodEnd) {

        return UsageAggregate.<UsageAggregate>find(
                "organization = ?1 and yearMonth = ?2",
                org,
                YearMonth.from(periodStart)).firstResult()
                .flatMap(aggregate -> {
                    if (aggregate != null && !aggregate.finalized) {
                        aggregate.costBreakdown.forEach((type, cost) -> {
                            if (cost.compareTo(BigDecimal.ZERO) > 0) {
                                InvoiceLineItem item = new InvoiceLineItem();
                                item.itemType = LineItemType.USAGE;
                                item.description = "Usage: " + type;
                                item.amount = cost;
                                item.periodStart = periodStart;
                                item.periodEnd = periodEnd;
                                item.usageAggregateId = aggregate.aggregateId;

                                invoice.addLineItem(item);
                            }
                        });

                        aggregate.finalized = true;
                        return aggregate.persist().map(v -> invoice);
                    } else {
                        return Uni.createFrom().item(invoice);
                    }
                });
    }

    private String generateInvoiceNumber() {
        String yearMonth = YearMonth.now().toString().replace("-", "");
        String random = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return "INV-" + yearMonth + "-" + random;
    }
}
