package tech.kayys.wayang.billing.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.billing.domain.UsageAggregate;
import tech.kayys.wayang.billing.domain.UsageRecord;
import tech.kayys.wayang.billing.exception.QuotaExceededException;

import tech.kayys.wayang.billing.dto.RecordUsageRequest;
import tech.kayys.wayang.billing.model.UsageType;
import tech.kayys.wayang.organization.domain.Organization;

@ApplicationScoped
public class UsageMeteringService {

    private static final Logger LOG = LoggerFactory.getLogger(UsageMeteringService.class);

    @Inject
    QuotaEnforcementService quotaService;

    @Inject
    CostCalculator costCalculator;

    // Batch buffer for high-throughput writes
    private final Map<String, List<UsageRecord>> batchBuffer = new ConcurrentHashMap<>();
    private static final int BATCH_SIZE = 100;
    private static final Duration BATCH_INTERVAL = Duration.ofSeconds(10);

    /**
     * Record usage (async, batched)
     */
    public Uni<Void> recordUsage(RecordUsageRequest request) {
        return Organization.<Organization>find("tenantId", request.tenantId())
                .firstResult()
                .flatMap(org -> {
                    if (org == null) {
                        return Uni.createFrom().failure(
                                new NoSuchElementException("Organization not found"));
                    }

                    // Check quota first
                    return quotaService.checkQuota(org, request.usageType(), request.quantity())
                            .flatMap(allowed -> {
                                if (!allowed) {
                                    return Uni.createFrom().failure(
                                            new QuotaExceededException(
                                                    "Quota exceeded for " + request.usageType()));
                                }

                                // Increment usage counter
                                return quotaService.incrementUsage(
                                        org,
                                        request.usageType(),
                                        request.quantity());
                            })
                            .flatMap(newTotal -> {
                                // Create usage record
                                UsageRecord record = createUsageRecord(org, request);

                                // Add to batch buffer
                                addToBatch(record);

                                return Uni.createFrom().voidItem();
                            });
                });
    }

    /**
     * Create usage record
     */
    private UsageRecord createUsageRecord(Organization org, RecordUsageRequest request) {
        UsageRecord record = new UsageRecord();
        record.organization = org;
        record.usageType = request.usageType();
        record.resourceId = request.resourceId();
        record.quantity = request.quantity();
        record.unit = request.unit();
        record.timestamp = Instant.now();

        // Set billing period
        Instant now = Instant.now();
        if (org.activeSubscription != null) {
            record.periodStart = org.activeSubscription.currentPeriodStart;
            record.periodEnd = org.activeSubscription.currentPeriodEnd;
        } else {
            // Use calendar month
            record.periodStart = YearMonth.now().atDay(1)
                    .atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
            record.periodEnd = YearMonth.now().atEndOfMonth()
                    .atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC);
        }

        // Calculate cost
        BigDecimal unitPrice = costCalculator.getUnitPrice(
                org,
                request.usageType());
        record.unitPrice = unitPrice;
        record.totalCost = unitPrice.multiply(BigDecimal.valueOf(request.quantity()));

        record.metadata = request.metadata() != null ? new HashMap<>(request.metadata()) : new HashMap<>();

        return record;
    }

    /**
     * Add to batch buffer
     */
    private void addToBatch(UsageRecord record) {
        String key = record.organization.tenantId + ":" +
                YearMonth.now().toString();

        batchBuffer.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(record);

        // Check if batch is full
        List<UsageRecord> batch = batchBuffer.get(key);
        if (batch.size() >= BATCH_SIZE) {
            flushBatch(key, new ArrayList<>(batch));
            batch.clear();
        }
    }

    /**
     * Flush batch to database
     */
    private void flushBatch(String key, List<UsageRecord> records) {
        if (records.isEmpty()) {
            return;
        }

        LOG.debug("Flushing batch of {} usage records for key: {}",
                records.size(), key);

        io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() -> Uni.join().all(
                records.stream()
                        .map(record -> record.persist())
                        .toList())
                .andFailFast())
                .subscribe().with(
                        v -> LOG.debug("Batch flushed successfully"),
                        error -> LOG.error("Error flushing batch", error));
    }

    /**
     * Scheduled batch flush
     */
    @Scheduled(every = "10s")
    public void scheduledFlush() {
        Map<String, List<UsageRecord>> snapshot = new HashMap<>(batchBuffer);
        batchBuffer.clear();

        snapshot.forEach((key, records) -> {
            if (!records.isEmpty()) {
                flushBatch(key, records);
            }
        });
    }

    /**
     * Get current period usage
     */
    public Uni<UsageAggregate> getCurrentPeriodUsage(
            UUID organizationId,
            YearMonth period) {

        return UsageAggregate.<UsageAggregate>find(
                "organization.organizationId = ?1 and yearMonth = ?2",
                organizationId,
                period).firstResult()
                .flatMap(aggregate -> {
                    if (aggregate != null) {
                        return Uni.createFrom().item(aggregate);
                    }

                    // Create new aggregate
                    return Organization.<Organization>findById(organizationId)
                            .flatMap(org -> createUsageAggregate(org, period));
                });
    }

    /**
     * Create usage aggregate
     */
    private Uni<UsageAggregate> createUsageAggregate(
            Organization org,
            YearMonth period) {

        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() -> {
            UsageAggregate aggregate = new UsageAggregate();
            aggregate.organization = org;
            aggregate.yearMonth = period;
            aggregate.computedAt = Instant.now();
            aggregate.finalized = false;

            return aggregate.persist()
                    .map(v -> aggregate);
        });
    }

    /**
     * Get usage history
     */
    public Uni<List<UsageAggregate>> getUsageHistory(
            UUID organizationId,
            YearMonth startMonth,
            YearMonth endMonth) {

        return UsageAggregate.<UsageAggregate>find(
                "organization.organizationId = ?1 and yearMonth >= ?2 and yearMonth <= ?3",
                organizationId,
                startMonth,
                endMonth).list();
    }

    /**
     * Get usage breakdown by type
     */
    public Uni<Map<UsageType, Long>> getUsageBreakdown(
            UUID organizationId,
            YearMonth period) {

        return UsageRecord.<UsageRecord>find(
                "organization.organizationId = ?1 and " +
                        "timestamp >= ?2 and timestamp < ?3",
                organizationId,
                period.atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                period.atEndOfMonth().atTime(23, 59, 59).toInstant(java.time.ZoneOffset.UTC)).list()
                .map(records -> {
                    Map<UsageType, Long> breakdown = new HashMap<>();

                    for (UsageRecord record : records) {
                        breakdown.merge(record.usageType, record.quantity, Long::sum);
                    }

                    return breakdown;
                });
    }
}