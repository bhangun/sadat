package tech.kayys.wayang.billing.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.billing.model.ChurnFeatures;
import tech.kayys.wayang.billing.model.HealthScore;
import tech.kayys.wayang.billing.model.HealthStatus;
import tech.kayys.wayang.organization.domain.Organization;

/**
 * Customer health scoring
 */
@ApplicationScoped
public class CustomerHealthService {
    
    private static final Logger LOG = LoggerFactory.getLogger(CustomerHealthService.class);
    
    @Inject
    FeatureEngineeringService featureService;
    
    /**
     * Calculate comprehensive health score
     */
    @CacheResult(cacheName = "health-scores")
    public Uni<HealthScore> calculateHealthScore(UUID organizationId) {
        LOG.info("Calculating health score for organization: {}", organizationId);
        return Organization.<Organization>findById(organizationId)
            .flatMap(org -> 
                featureService.extractFeatures(org)
                    .map(features -> calculateScore(features))
            );
    }
    
    private HealthScore calculateScore(ChurnFeatures features) {
        // Weighted scoring model
        double score = 0.0;
        score += features.engagementScore * 0.25;  // 25% weight
        score += (features.usageTrend + 1.0) * 50 * 0.20;  // 20% weight
        score += features.billingHealth * 0.20;  // 20% weight
        score += (100 - features.supportScore) * 0.15;  // 15% weight
        score += features.productAdoption * 0.20;  // 20% weight
        
        // Penalties
        if (features.hasFailedPayments) score -= 15;
        if (features.isTrialUser) score -= 5;
        
        score = Math.max(0, Math.min(100, score));
        
        return new HealthScore(
            features.organizationId,
            score,
            determineHealthStatus(score),
            Instant.now(),
            Map.of(
                "engagement", features.engagementScore,
                "usage", (features.usageTrend + 1.0) * 50,
                "billing", features.billingHealth,
                "support", 100 - features.supportScore,
                "adoption", features.productAdoption
            )
        );
    }
    
    private HealthStatus determineHealthStatus(double score) {
        if (score >= 80) return HealthStatus.HEALTHY;
        if (score >= 60) return HealthStatus.MODERATE;
        if (score >= 40) return HealthStatus.AT_RISK;
        return HealthStatus.CRITICAL;
    }
}