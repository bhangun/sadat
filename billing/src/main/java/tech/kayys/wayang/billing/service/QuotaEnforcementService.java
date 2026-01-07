package tech.kayys.wayang.billing.service;


import io.quarkus.cache.CacheResult;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.billing.domain.ResourceQuotas;
import tech.kayys.wayang.billing.model.QuotaStatus;
import tech.kayys.wayang.billing.model.UsageType;
import tech.kayys.wayang.organization.domain.Organization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================================
 * SILAT QUOTA & METERING ENGINE
 * ============================================================================
 * 
 * High-performance quota enforcement and usage metering:
 * - Real-time quota checking with Redis
 * - Distributed rate limiting
 * - Usage aggregation pipeline
 * - Cost calculation engine
 * - Alert thresholds
 */

// ==================== QUOTA ENFORCEMENT SERVICE ====================

@ApplicationScoped
public class QuotaEnforcementService {
    
    private static final Logger LOG = LoggerFactory.getLogger(QuotaEnforcementService.class);
    
    @Inject
    RedisDataSource redisDataSource;
    
    @Inject
    AlertService alertService;
    
    private ValueCommands<String, Long> redis;
    
    @jakarta.annotation.PostConstruct
    void init() {
        this.redis = redisDataSource.value(Long.class);
    }
    
    /**
     * Check if usage is within quota (fast path with Redis)
     */
    public Uni<Boolean> checkQuota(
            Organization org,
            UsageType usageType,
            long quantity) {
        
        return getCurrentPeriodUsage(org, usageType)
            .flatMap(currentUsage -> {
                long limit = getQuotaLimit(org, usageType);
                long newTotal = currentUsage + quantity;
                
                if (newTotal > limit) {
                    // Check if we should alert
                    return checkAndSendAlerts(org, usageType, currentUsage, limit)
                        .replaceWith(false);
                } else if (newTotal > limit * 0.8) { // 80% threshold
                    // Warning alert
                    return alertService.sendQuotaWarning(
                        org, 
                        usageType, 
                        (double) newTotal / limit * 100
                    ).replaceWith(true);
                }
                
                return Uni.createFrom().item(true);
            });
    }
    
    /**
     * Increment usage counter (atomic operation in Redis)
     */
    public Uni<Long> incrementUsage(
            Organization org,
            UsageType usageType,
            long quantity) {
        
        String key = buildUsageKey(org.tenantId, usageType, YearMonth.now());
        
        return Uni.createFrom().item(() -> {
            Long newValue = redis.incrby(key, quantity);
            
            // Set expiration if new key
            if (newValue == quantity) {
                redis.expire(key, Duration.ofDays(90));
            }
            
            return newValue;
        });
    }
    
    /**
     * Get current period usage from Redis
     */
    public Uni<Long> getCurrentPeriodUsage(Organization org, UsageType usageType) {
        String key = buildUsageKey(org.tenantId, usageType, YearMonth.now());
        
        return Uni.createFrom().item(() -> {
            Long usage = redis.get(key);
            return usage != null ? usage : 0L;
        });
    }
    
    /**
     * Get quota limit based on subscription plan
     */
    private long getQuotaLimit(Organization org, UsageType usageType) {
        if (org.activeSubscription == null) {
            return getDefaultQuota(usageType);
        }
        
        ResourceQuotas quotas = org.activeSubscription.plan.quotas;
        
        return switch (usageType) {
            case WORKFLOW_EXECUTION -> quotas.maxWorkflowRunsPerMonth;
            case AI_TOKEN_USAGE -> quotas.maxAiTokensPerMonth;
            case API_CALL -> quotas.maxApiCallsPerMonth;
            case STORAGE_GB_HOUR -> quotas.maxStorageGb * 720L; // GB-hours per month
            default -> Long.MAX_VALUE;
        };
    }
    
    private long getDefaultQuota(UsageType usageType) {
        return switch (usageType) {
            case WORKFLOW_EXECUTION -> 100L;
            case AI_TOKEN_USAGE -> 10000L;
            case API_CALL -> 10000L;
            case STORAGE_GB_HOUR -> 720L; // 1 GB
            default -> 1000L;
        };
    }
    
    /**
     * Check and send alerts
     */
    private Uni<Void> checkAndSendAlerts(
            Organization org,
            UsageType usageType,
            long currentUsage,
            long limit) {
        
        double percentUsed = (double) currentUsage / limit * 100;
        
        if (percentUsed >= 100) {
            return alertService.sendQuotaExceeded(org, usageType, currentUsage, limit);
        } else if (percentUsed >= 90) {
            return alertService.sendQuotaCritical(org, usageType, percentUsed);
        } else if (percentUsed >= 80) {
            return alertService.sendQuotaWarning(org, usageType, percentUsed);
        }
        
        return Uni.createFrom().voidItem();
    }
    
    /**
     * Build Redis key for usage tracking
     */
    private String buildUsageKey(String tenantId, UsageType type, YearMonth period) {
        return String.format("usage:%s:%s:%s", tenantId, type, period);
    }
    
    /**
     * Get quota status for organization
     */
    public Uni<Map<String, QuotaStatus>> getQuotaStatus(Organization org) {
        Map<String, QuotaStatus> status = new HashMap<>();
        
        List<Uni<Void>> checks = new ArrayList<>();
        
        for (UsageType type : UsageType.values()) {
            checks.add(
                getCurrentPeriodUsage(org, type)
                    .map(used -> {
                        long limit = getQuotaLimit(org, type);
                        status.put(
                            type.name(),
                            new QuotaStatus(used, limit)
                        );
                        return null;
                    })
            );
        }
        
        return Uni.join().all(checks).andFailFast()
            .replaceWith(status);
    }
}