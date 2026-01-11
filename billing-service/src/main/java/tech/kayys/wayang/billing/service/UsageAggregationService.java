package tech.kayys.wayang.billing.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.billing.domain.ResourceQuotas;
import tech.kayys.wayang.billing.domain.UsageAggregate;
import tech.kayys.wayang.billing.domain.UsageRecord;
import tech.kayys.wayang.billing.model.QuotaStatus;
import tech.kayys.wayang.billing.model.UsageType;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.organization.model.OrganizationStatus;

@ApplicationScoped
public class UsageAggregationService {
    
    private static final Logger LOG = LoggerFactory.getLogger(UsageAggregationService.class);
    
    @Inject
    CostCalculator costCalculator;
    
    /**
     * Aggregate usage for billing period (scheduled)
     */
    @Scheduled(every = "1h")
    public void aggregateUsage() {
        LOG.info("Running usage aggregation");
        
        YearMonth currentMonth = YearMonth.now();
        
        Organization.<Organization>find("status", OrganizationStatus.ACTIVE)
            .list()
            .subscribe().with(
                organizations -> organizations.forEach(org -> 
                    aggregateForOrganization(org, currentMonth)),
                error -> LOG.error("Error aggregating usage", error)
            );
    }
    
    /**
     * Aggregate usage for single organization
     */
    private void aggregateForOrganization(Organization org, YearMonth period) {
        LOG.debug("Aggregating usage for org: {} period: {}", 
            org.tenantId, period);
        
        Instant periodStart = period.atDay(1)
            .atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        Instant periodEnd = period.atEndOfMonth()
            .atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC);
        
        // Get or create aggregate
        UsageAggregate.<UsageAggregate>find(
            "organization = ?1 and yearMonth = ?2",
            org,
            period
        ).firstResult()
        .flatMap(aggregate -> {
            if (aggregate == null) {
                aggregate = new UsageAggregate();
                aggregate.organization = org;
                aggregate.yearMonth = period;
            }
            return Uni.createFrom().item(aggregate);
        })
        .flatMap(aggregate ->
            // Get usage records
            UsageRecord.<UsageRecord>find(
                "organization = ?1 and timestamp >= ?2 and timestamp < ?3 and isBilled = false",
                org,
                periodStart,
                periodEnd
            ).list()
            .map(records -> {
                // Aggregate by type
                Map<UsageType, Long> usageByType = new HashMap<>();
                Map<String, BigDecimal> costBreakdown = new HashMap<>();
                BigDecimal totalCost = BigDecimal.ZERO;
                
                for (UsageRecord record : records) {
                    usageByType.merge(record.usageType, record.quantity, Long::sum);
                    
                    String costKey = record.usageType.name();
                    costBreakdown.merge(costKey, record.totalCost, BigDecimal::add);
                    totalCost = totalCost.add(record.totalCost);
                }
                
                aggregate.usageByType = usageByType;
                aggregate.costBreakdown = costBreakdown;
                aggregate.totalCost = totalCost;
                aggregate.computedAt = Instant.now();
                
                // Calculate quota status
                Map<String, QuotaStatus> quotaStatus = new HashMap<>();
                for (UsageType type : UsageType.values()) {
                    long used = usageByType.getOrDefault(type, 0L);
                    long limit = getQuotaLimit(org, type);
                    quotaStatus.put(type.name(), new QuotaStatus(used, limit));
                }
                aggregate.quotaStatus = quotaStatus;
                
                return aggregate;
            })
        )
        .flatMap(aggregate ->
            io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() ->
                aggregate.persist()
            )
        )
        .subscribe().with(
            v -> LOG.debug("Usage aggregated for org: {}", org.tenantId),
            error -> LOG.error("Error aggregating for org: {}", org.tenantId, error)
        );
    }
    
    private long getQuotaLimit(Organization org, UsageType usageType) {
        if (org.activeSubscription == null) {
            return Long.MAX_VALUE;
        }
        
        ResourceQuotas quotas = org.activeSubscription.plan.quotas;
        
        return switch (usageType) {
            case WORKFLOW_EXECUTION -> quotas.maxWorkflowRunsPerMonth;
            case AI_TOKEN_USAGE -> quotas.maxAiTokensPerMonth;
            case API_CALL -> quotas.maxApiCallsPerMonth;
            case STORAGE_GB_HOUR -> quotas.maxStorageGb * 720L;
            default -> Long.MAX_VALUE;
        };
    }
}

