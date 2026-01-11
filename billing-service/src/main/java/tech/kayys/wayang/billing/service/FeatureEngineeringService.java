package tech.kayys.wayang.billing.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.billing.domain.UsageRecord;
import tech.kayys.wayang.billing.model.ChurnFeatures;
import tech.kayys.wayang.billing.model.UsageType;
import tech.kayys.wayang.invoice.domain.Invoice;
import tech.kayys.wayang.invoice.model.InvoiceStatus;
import tech.kayys.wayang.organization.domain.Organization;

/**
 * Feature engineering for ML models
 */
@ApplicationScoped
public class FeatureEngineeringService {

    private static final Logger LOG = LoggerFactory.getLogger(FeatureEngineeringService.class);

    /**
     * Extract features for churn prediction
     */
    public Uni<ChurnFeatures> extractFeatures(Organization org) {
        LOG.info("Extracting features for organization {}", org.organizationId);
        return Uni.combine().all()
                .unis(
                        calculateEngagementScore(org),
                        calculateUsageTrend(org),
                        calculateBillingHealth(org),
                        calculateSupportScore(org),
                        calculateProductAdoption(org),
                        calculateTenure(org))
                .asTuple()
                .map(tuple -> {
                    ChurnFeatures features = new ChurnFeatures();
                    features.organizationId = org.organizationId;
                    features.engagementScore = tuple.getItem1();
                    features.usageTrend = tuple.getItem2();
                    features.billingHealth = tuple.getItem3();
                    features.supportScore = tuple.getItem4();
                    features.productAdoption = tuple.getItem5();
                    features.tenureMonths = tuple.getItem6();

                    // Additional derived features
                    features.planTier = org.activeSubscription != null ? org.activeSubscription.plan.tier.ordinal() : 0;
                    features.hasFailedPayments = hasRecentFailedPayments(org);
                    features.isTrialUser = org.activeSubscription != null &&
                            org.activeSubscription.isTrial;

                    return features;
                });
    }

    /**
     * Engagement score (0-100)
     */
    private Uni<Double> calculateEngagementScore(Organization org) {
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

        return UsageRecord.<UsageRecord>find(
                "organization = ?1 and timestamp >= ?2",
                org, thirtyDaysAgo).count()
                .map(count -> {
                    // Normalize to 0-100
                    double score = Math.min(100.0, (count / 100.0) * 100);
                    return score;
                });
    }

    /**
     * Usage trend (-1.0 to 1.0, negative = declining)
     */
    private Uni<Double> calculateUsageTrend(Organization org) {
        Instant now = Instant.now();
        Instant thirtyDaysAgo = now.minus(30, ChronoUnit.DAYS);
        Instant sixtyDaysAgo = now.minus(60, ChronoUnit.DAYS);

        return Uni.combine().all()
                .unis(
                        UsageRecord.count("organization = ?1 and timestamp >= ?2 and timestamp < ?3",
                                org, thirtyDaysAgo, now),
                        UsageRecord.count("organization = ?1 and timestamp >= ?2 and timestamp < ?3",
                                org, sixtyDaysAgo, thirtyDaysAgo))
                .asTuple()
                .map(tuple -> {
                    long recentCount = tuple.getItem1();
                    long previousCount = tuple.getItem2();

                    if (previousCount == 0)
                        return 0.0;

                    double trend = ((double) recentCount - previousCount) / previousCount;
                    return Math.max(-1.0, Math.min(1.0, trend));
                });
    }

    /**
     * Billing health score (0-100)
     */
    private Uni<Double> calculateBillingHealth(Organization org) {
        Instant sixMonthsAgo = Instant.now().minus(180, ChronoUnit.DAYS);

        return Invoice.<Invoice>find(
                "organization = ?1 and invoiceDate >= ?2",
                org, sixMonthsAgo).list()
                .map(invoices -> {
                    if (invoices.isEmpty())
                        return 100.0;

                    long onTimePayments = invoices.stream()
                            .filter(inv -> inv.status == InvoiceStatus.PAID &&
                                    inv.paidAt != null &&
                                    inv.paidAt.isBefore(inv.dueDate))
                            .count();

                    double onTimeRate = (double) onTimePayments / invoices.size();
                    return onTimeRate * 100;
                });
    }

    /**
     * Support interaction score (0-100, higher = more issues)
     */
    private Uni<Double> calculateSupportScore(Organization org) {
        // TODO: Integrate with support ticket system
        return Uni.createFrom().item(50.0);
    }

    /**
     * Product adoption score (0-100)
     */
    private Uni<Double> calculateProductAdoption(Organization org) {
        // Check which features are being used
        Set<String> usedFeatures = new HashSet<>();

        if (org.hasFeature("workflow_engine"))
            usedFeatures.add("workflows");
        if (org.hasFeature("ai_agents"))
            usedFeatures.add("ai");
        if (org.hasFeature("control_plane"))
            usedFeatures.add("control_plane");

        // Check actual usage
        return UsageRecord.<UsageRecord>find(
                "organization = ?1 and timestamp >= ?2",
                org,
                Instant.now().minus(30, ChronoUnit.DAYS)).list()
                .map(records -> {
                    Set<UsageType> usedTypes = records.stream()
                            .map(r -> r.usageType)
                            .collect(Collectors.toSet());

                    double featureUsageRate = usedTypes.isEmpty() ? 0
                            : (double) usedTypes.size() / UsageType.values().length;

                    return featureUsageRate * 100;
                });
    }

    /**
     * Tenure in months
     */
    private Uni<Integer> calculateTenure(Organization org) {
        long months = ChronoUnit.MONTHS.between(
                org.createdAt,
                Instant.now());
        return Uni.createFrom().item((int) months);
    }

    private boolean hasRecentFailedPayments(Organization org) {
        // Check last 90 days for failed payments
        return false; // TODO: Implement
    }
}
