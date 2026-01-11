package tech.kayys.wayang.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.invoice.domain.Invoice;
import tech.kayys.wayang.invoice.model.InvoiceStatus;
import tech.kayys.wayang.organization.domain.Organization;

/**
 * Customer value calculator
 */
@ApplicationScoped
class CustomerValueCalculator {

    /**
     * Calculate Customer Lifetime Value
     */
    public Uni<Double> calculateCLV(Organization org) {
        return Uni.combine().all()
                .unis(
                        calculateAverageRevenue(org),
                        calculateRetentionRate(org),
                        calculateCustomerLifespan(org))
                .asTuple()
                .map(tuple -> {
                    double avgRevenue = tuple.getItem1();
                    double retentionRate = tuple.getItem2();
                    int lifespan = tuple.getItem3();

                    // CLV = (Average Revenue per Month × Retention Rate) × Lifespan
                    return avgRevenue * retentionRate * lifespan;
                });
    }

    private Uni<Double> calculateAverageRevenue(Organization org) {
        return Invoice.<Invoice>find(
                "organization = ?1 and status = ?2",
                org,
                InvoiceStatus.PAID).list()
                .map(invoices -> {
                    if (invoices.isEmpty())
                        return 0.0;

                    BigDecimal total = invoices.stream()
                            .map(inv -> inv.totalAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return total.divide(
                            BigDecimal.valueOf(invoices.size()),
                            2,
                            RoundingMode.HALF_UP).doubleValue();
                });
    }

    private Uni<Double> calculateRetentionRate(Organization org) {
        // Simplified: based on tenure
        long months = ChronoUnit.MONTHS.between(org.createdAt, Instant.now());
        return Uni.createFrom().item(Math.min(0.95, 0.6 + (months * 0.02)));
    }

    private Uni<Integer> calculateCustomerLifespan(Organization org) {
        // Average customer lifespan in months
        long tenure = ChronoUnit.MONTHS.between(org.createdAt, Instant.now());
        return Uni.createFrom().item((int) Math.max(12, tenure * 2));
    }
}