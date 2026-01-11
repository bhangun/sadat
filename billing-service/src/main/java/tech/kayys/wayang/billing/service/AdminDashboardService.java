package tech.kayys.wayang.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.billing.domain.ResourceAllocation;
import tech.kayys.wayang.billing.domain.UsageRecord;
import tech.kayys.wayang.billing.dto.PlatformOverview;
import tech.kayys.wayang.billing.dto.RevenueMetrics;
import tech.kayys.wayang.billing.dto.TenantHealthOverview;
import tech.kayys.wayang.billing.dto.UsageMetrics;
import tech.kayys.wayang.billing.model.AllocationStatus;
import tech.kayys.wayang.billing.model.UsageType;
import tech.kayys.wayang.invoice.domain.Invoice;
import tech.kayys.wayang.invoice.model.InvoiceStatus;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.organization.domain.OrganizationUser;
import tech.kayys.wayang.organization.model.OrganizationStatus;
import tech.kayys.wayang.subscription.domain.Subscription;
import tech.kayys.wayang.subscription.model.SubscriptionStatus;

@ApplicationScoped
public class AdminDashboardService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminDashboardService.class);

    /**
     * Get platform overview
     */
    public Uni<PlatformOverview> getPlatformOverview() {
        return Uni.combine().all()
                .unis(
                        getTotalOrganizations(),
                        getActiveOrganizations(),
                        getTotalSubscriptions(),
                        getActiveSubscriptions(),
                        getTotalRevenueMRR(),
                        getTotalRevenueARR(),
                        getAverageRevenuePerUser(),
                        getChurnRate(),
                        getTotalUsers(),
                        getTotalWorkflowRuns())
                .with(list -> new PlatformOverview(
                        (Long) list.get(0),
                        (Long) list.get(1),
                        (Long) list.get(2),
                        (Long) list.get(3),
                        (BigDecimal) list.get(4),
                        (BigDecimal) list.get(5),
                        (BigDecimal) list.get(6),
                        (Double) list.get(7),
                        (Long) list.get(8),
                        (Long) list.get(9)));
    }

    /**
     * Get revenue metrics
     */
    public Uni<RevenueMetrics> getRevenueMetrics(
            YearMonth startMonth,
            YearMonth endMonth) {

        return Uni.combine().all()
                .unis(
                        getRevenueByMonth(startMonth, endMonth),
                        getRevenueByPlan(startMonth, endMonth),
                        getNewSubscriptionsRevenue(startMonth, endMonth),
                        getChurnedRevenue(startMonth, endMonth),
                        getExpansionRevenue(startMonth, endMonth))
                .combinedWith(list -> new RevenueMetrics(
                        (Map<YearMonth, BigDecimal>) list.get(0), // monthly revenue
                        (Map<String, BigDecimal>) list.get(1), // by plan
                        (BigDecimal) list.get(2), // new
                        (BigDecimal) list.get(3), // churned
                        (BigDecimal) list.get(4) // expansion
                ));
    }

    /**
     * Get usage metrics
     */
    public Uni<UsageMetrics> getUsageMetrics() {
        return Uni.combine().all()
                .unis(
                        getTotalWorkflowExecutions(),
                        getTotalAITokens(),
                        getTotalApiCalls(),
                        getTotalStorageUsed(),
                        getUsageByType())
                .combinedWith(list -> new UsageMetrics(
                        (Long) list.get(0),
                        (Long) list.get(1),
                        (Long) list.get(2),
                        (Long) list.get(3),
                        (Map<UsageType, Long>) list.get(4)));
    }

    /**
     * Get tenant health overview
     */
    public Uni<TenantHealthOverview> getTenantHealthOverview() {
        return Uni.combine().all()
                .unis(
                        getHealthyTenants(),
                        getAtRiskTenants(),
                        getTenantsByStatus(),
                        getAverageUptime())
                .combinedWith(list -> new TenantHealthOverview(
                        (Long) list.get(0),
                        (Long) list.get(1),
                        (Map<OrganizationStatus, Long>) list.get(2),
                        (Double) list.get(3)));
    }

    /**
     * Search organizations
     */
    public Uni<List<Organization>> searchOrganizations(String query) {
        LOG.debug("Searching organizations with query: {}", query);
        return Organization.<Organization>find(
                "lower(name) like ?1 or lower(slug) like ?1 or tenantId like ?1",
                "%" + query.toLowerCase() + "%").list();
    }

    // Helper methods for metrics calculation

    private Uni<Long> getTotalOrganizations() {
        return Organization.count("deletedAt is null");
    }

    private Uni<Long> getActiveOrganizations() {
        return Organization.count("status = ?1 and deletedAt is null",
                OrganizationStatus.ACTIVE);
    }

    private Uni<Long> getTotalSubscriptions() {
        return Subscription.count();
    }

    private Uni<Long> getActiveSubscriptions() {
        return Subscription.count("status = ?1", SubscriptionStatus.ACTIVE);
    }

    private Uni<BigDecimal> getTotalRevenueMRR() {
        return Subscription.<Subscription>find("status = ?1", SubscriptionStatus.ACTIVE)
                .list()
                .map(subscriptions -> {
                    BigDecimal mrr = BigDecimal.ZERO;
                    for (Subscription sub : subscriptions) {
                        BigDecimal monthlyAmount = calculateMonthlyRevenue(sub);
                        mrr = mrr.add(monthlyAmount);
                    }
                    return mrr;
                });
    }

    private Uni<BigDecimal> getTotalRevenueARR() {
        return getTotalRevenueMRR()
                .map(mrr -> mrr.multiply(BigDecimal.valueOf(12)));
    }

    private Uni<BigDecimal> getAverageRevenuePerUser() {
        return Uni.combine().all()
                .unis(getTotalRevenueMRR(), getActiveSubscriptions())
                .combinedWith(list -> {
                    BigDecimal mrr = (BigDecimal) list.get(0);
                    Long subs = (Long) list.get(1);
                    return subs > 0 ? mrr.divide(BigDecimal.valueOf(subs), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                });
    }

    private Uni<Double> getChurnRate() {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        return Uni.combine().all()
                .unis(
                        Subscription.count("cancelledAt >= ?1", thirtyDaysAgo),
                        Subscription.count("createdAt < ?1", thirtyDaysAgo))
                .combinedWith(list -> {
                    long churned = (Long) list.get(0);
                    long total = (Long) list.get(1);
                    return total > 0 ? (double) churned / total * 100 : 0.0;
                });
    }

    private Uni<Long> getTotalUsers() {
        return OrganizationUser.count();
    }

    private Uni<Long> getTotalWorkflowRuns() {
        // Query workflow engine
        return Uni.createFrom().item(0L);
    }

    private Uni<Map<YearMonth, BigDecimal>> getRevenueByMonth(
            YearMonth start,
            YearMonth end) {

        return Invoice.<Invoice>find(
                "invoiceDate >= ?1 and invoiceDate <= ?2 and status = ?3",
                start.atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                end.atEndOfMonth().atTime(23, 59).toInstant(java.time.ZoneOffset.UTC),
                InvoiceStatus.PAID).list()
                .map(invoices -> {
                    Map<YearMonth, BigDecimal> revenue = new TreeMap<>();

                    for (Invoice invoice : invoices) {
                        YearMonth month = YearMonth.from(
                                invoice.invoiceDate.atZone(java.time.ZoneOffset.UTC));
                        revenue.merge(month, invoice.totalAmount, BigDecimal::add);
                    }

                    return revenue;
                });
    }

    private Uni<Map<String, BigDecimal>> getRevenueByPlan(
            YearMonth start,
            YearMonth end) {

        return Subscription.<Subscription>find("status = ?1", SubscriptionStatus.ACTIVE)
                .list()
                .map(subscriptions -> {
                    Map<String, BigDecimal> byPlan = new HashMap<>();

                    for (Subscription sub : subscriptions) {
                        BigDecimal monthly = calculateMonthlyRevenue(sub);
                        byPlan.merge(sub.plan.name, monthly, BigDecimal::add);
                    }

                    return byPlan;
                });
    }

    private Uni<BigDecimal> getNewSubscriptionsRevenue(
            YearMonth start,
            YearMonth end) {

        return Subscription.<Subscription>find(
                "createdAt >= ?1 and createdAt <= ?2",
                start.atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                end.atEndOfMonth().atTime(23, 59).toInstant(java.time.ZoneOffset.UTC)).list()
                .map(subscriptions -> {
                    BigDecimal revenue = BigDecimal.ZERO;
                    for (Subscription sub : subscriptions) {
                        revenue = revenue.add(calculateMonthlyRevenue(sub));
                    }
                    return revenue;
                });
    }

    private Uni<BigDecimal> getChurnedRevenue(YearMonth start, YearMonth end) {
        return Subscription.<Subscription>find(
                "cancelledAt >= ?1 and cancelledAt <= ?2",
                start.atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                end.atEndOfMonth().atTime(23, 59).toInstant(java.time.ZoneOffset.UTC)).list()
                .map(subscriptions -> {
                    BigDecimal revenue = BigDecimal.ZERO;
                    for (Subscription sub : subscriptions) {
                        revenue = revenue.add(calculateMonthlyRevenue(sub));
                    }
                    return revenue;
                });
    }

    private Uni<BigDecimal> getExpansionRevenue(YearMonth start, YearMonth end) {
        // Calculate expansion from plan upgrades
        return Uni.createFrom().item(BigDecimal.ZERO);
    }

    private BigDecimal calculateMonthlyRevenue(Subscription sub) {
        return switch (sub.billingCycle) {
            case MONTHLY -> sub.basePrice;
            case QUARTERLY -> sub.basePrice.divide(
                    BigDecimal.valueOf(3), 2, RoundingMode.HALF_UP);
            case ANNUAL -> sub.basePrice.divide(
                    BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        };
    }

    private Uni<Long> getTotalWorkflowExecutions() {
        return UsageRecord.count("usageType = ?1", UsageType.WORKFLOW_EXECUTION);
    }

    private Uni<Long> getTotalAITokens() {
        return UsageRecord.<UsageRecord>find("usageType = ?1", UsageType.AI_TOKEN_USAGE)
                .list()
                .map(records -> records.stream()
                        .mapToLong(r -> r.quantity)
                        .sum());
    }

    private Uni<Long> getTotalApiCalls() {
        return UsageRecord.count("usageType = ?1", UsageType.API_CALL);
    }

    private Uni<Long> getTotalStorageUsed() {
        return UsageRecord.<UsageRecord>find("usageType = ?1", UsageType.STORAGE_GB_HOUR)
                .list()
                .map(records -> records.stream()
                        .mapToLong(r -> r.quantity)
                        .sum() / 720 // Convert GB-hours to GB
                );
    }

    private Uni<Map<UsageType, Long>> getUsageByType() {
        return UsageRecord.<UsageRecord>findAll()
                .list()
                .map(records -> {
                    Map<UsageType, Long> byType = new HashMap<>();
                    for (UsageRecord record : records) {
                        byType.merge(record.usageType, record.quantity, Long::sum);
                    }
                    return byType;
                });
    }

    private Uni<Long> getHealthyTenants() {
        return ResourceAllocation.count(
                "status = ?1 and healthStatus = ?2",
                AllocationStatus.ACTIVE,
                "HEALTHY");
    }

    private Uni<Long> getAtRiskTenants() {
        return Organization.count(
                "status = ?1 or (activeSubscription.cancelAtPeriodEnd = true)",
                OrganizationStatus.DELINQUENT);
    }

    private Uni<Map<OrganizationStatus, Long>> getTenantsByStatus() {
        return Organization.<Organization>findAll()
                .list()
                .map(orgs -> orgs.stream()
                        .collect(Collectors.groupingBy(
                                org -> org.status,
                                Collectors.counting())));
    }

    private Uni<Double> getAverageUptime() {
        return Uni.createFrom().item(99.9); // Calculate from health checks
    }
}
