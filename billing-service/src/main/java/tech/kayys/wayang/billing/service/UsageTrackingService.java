package tech.kayys.wayang.billing.service;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.billing.domain.UsageAggregate;
import tech.kayys.wayang.billing.domain.UsageRecord;
import tech.kayys.wayang.billing.dto.RecordUsageRequest;
import tech.kayys.wayang.billing.exception.QuotaExceededException;
import tech.kayys.wayang.billing.model.QuotaStatus;
import tech.kayys.wayang.billing.model.UsageType;
import tech.kayys.wayang.organization.domain.Organization;

@ApplicationScoped
public class UsageTrackingService {

    private static final Logger LOG = LoggerFactory.getLogger(UsageTrackingService.class);

    @Inject
    QuotaEnforcementService quotaService;

    @Inject
    TenantEventPublisher eventPublisher;

    @Inject
    UsageMeteringService meteringService;

    // In-memory buffer for high-throughput usage recording
    private final Map<String, List<UsageRecord>> usageBuffer = new ConcurrentHashMap<>();

    /**
     * Record usage event (high-throughput)
     */
    public Uni<Void> recordUsage(RecordUsageRequest request) {
        return Organization.<Organization>find("tenantId", request.tenantId())
            .firstResult()
            .flatMap(org -> {
                if (org == null) {
                    return Uni.createFrom().failure(
                        new NoSuchElementException("Organization not found"));
                }

                // Check quota before recording
                return quotaService.checkQuota(org, request.usageType(), request.quantity())
                    .flatMap(allowed -> {
                        if (!allowed) {
                            return eventPublisher.publishQuotaExceeded(
                                org,
                                request.usageType()
                            ).flatMap(v -> Uni.createFrom().failure(
                                new QuotaExceededException(
                                    "Quota exceeded for " + request.usageType())));
                        }
                        // Record the usage
                        return meteringService.recordUsage(request).replaceWithVoid();
                    });
            });
    }

    /**
     * Get current period usage
     */
    public Uni<UsageAggregate> getCurrentPeriodUsage(UUID organizationId, YearMonth yearMonth) {
        return Organization.<Organization>find("organizationId", organizationId)
            .firstResult()
            .flatMap(org -> {
                if (org == null) {
                    return Uni.createFrom().failure(
                        new NoSuchElementException("Organization not found"));
                }
                return meteringService.getCurrentPeriodUsage(org.organizationId, yearMonth);
            });
    }

    /**
     * Get usage history
     */
    public Uni<List<UsageAggregate>> getUsageHistory(
            UUID organizationId,
            YearMonth startMonth,
            YearMonth endMonth) {
        return Organization.<Organization>find("organizationId", organizationId)
            .firstResult()
            .flatMap(org -> {
                if (org == null) {
                    return Uni.createFrom().failure(
                        new NoSuchElementException("Organization not found"));
                }
                return meteringService.getUsageHistory(org.organizationId, startMonth, endMonth);
            });
    }

    /**
     * Get usage breakdown
     */
    public Uni<Map<UsageType, Long>> getUsageBreakdown(
            UUID organizationId,
            YearMonth yearMonth) {
        return Organization.<Organization>find("organizationId", organizationId)
            .firstResult()
            .flatMap(org -> {
                if (org == null) {
                    return Uni.createFrom().failure(
                        new NoSuchElementException("Organization not found"));
                }
                return meteringService.getUsageBreakdown(org.organizationId, yearMonth);
            });
    }

    /**
     * Get quota status
     */
    public Uni<Map<String, QuotaStatus>> getQuotaStatus(UUID organizationId) {
        return Organization.<Organization>find("organizationId", organizationId)
            .firstResult()
            .flatMap(org -> {
                if (org == null) {
                    return Uni.createFrom().failure(
                        new NoSuchElementException("Organization not found"));
                }
                return quotaService.getQuotaStatus(org);
            });
    }
}