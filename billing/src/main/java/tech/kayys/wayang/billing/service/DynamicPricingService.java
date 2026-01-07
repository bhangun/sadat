package tech.kayys.wayang.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.billing.domain.PersonalizedPricing;
import tech.kayys.wayang.billing.domain.UsageRecord;
import tech.kayys.wayang.billing.model.CompanyProfile;
import tech.kayys.wayang.billing.model.CompanySize;
import tech.kayys.wayang.billing.model.MarketPricing;
import tech.kayys.wayang.billing.model.PlanTier;
import tech.kayys.wayang.billing.model.PricingFactor;
import tech.kayys.wayang.billing.model.UsageProfile;
import tech.kayys.wayang.organization.domain.Organization;

/**
 * Dynamic pricing service
 */
@ApplicationScoped
public class DynamicPricingService {
    
    private static final Logger LOG = LoggerFactory.getLogger(DynamicPricingService.class);
    
    @Inject
    CustomerValueCalculator valueCalculator;
    
    @Inject
    CompetitiveIntelligence competitiveIntel;
    
    /**
     * Generate personalized pricing
     */
    public Uni<PersonalizedPricing> generateCustomPricing(UUID organizationId) {
        LOG.info("Generating personalized pricing for: {}", organizationId);
        
        return Organization.<Organization>findById(organizationId)
            .flatMap(org -> {
                if (org == null || org.activeSubscription == null) {
                    return Uni.createFrom().failure(
                        new IllegalArgumentException("Organization or subscription not found"));
                }
                
                return Uni.combine().all()
                    .unis(
                        valueCalculator.calculateCLV(org),
                        analyzeUsagePatterns(org),
                        assessCompanyProfile(org),
                        competitiveIntel.getMarketPricing(org)
                    )
                    .asTuple()
                    .map(tuple -> {
                        double clv = tuple.getItem1();
                        UsageProfile usageProfile = tuple.getItem2();
                        CompanyProfile companyProfile = tuple.getItem3();
                        MarketPricing marketPricing = tuple.getItem4();
                        
                        return calculatePersonalizedPrice(
                            org,
                            clv,
                            usageProfile,
                            companyProfile,
                            marketPricing
                        );
                    });
            })
            .flatMap(pricing ->
                io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() ->
                    pricing.persist().map(v -> pricing)
                )
            );
    }
    
    /**
     * Calculate personalized price
     */
    private PersonalizedPricing calculatePersonalizedPrice(
            Organization org,
            double clv,
            UsageProfile usageProfile,
            CompanyProfile companyProfile,
            MarketPricing marketPricing) {
        
        BigDecimal basePrice = org.activeSubscription.basePrice;
        Map<String, PricingFactor> factors = new HashMap<>();
        double totalImpact = 0.0;
        
        // Customer Lifetime Value factor
        double clvImpact = calculateCLVImpact(clv, basePrice.doubleValue());
        factors.put("customer_value", new PricingFactor(
            "Customer Lifetime Value",
            clvImpact,
            String.format("Estimated CLV: $%.2f", clv)
        ));
        totalImpact += clvImpact;
        
        // Usage intensity factor
        double usageImpact = calculateUsageImpact(usageProfile);
        factors.put("usage_intensity", new PricingFactor(
            "Usage Intensity",
            usageImpact,
            String.format("%.0f%% above average usage", usageImpact * 100)
        ));
        totalImpact += usageImpact;
        
        // Company size factor
        double sizeImpact = calculateSizeImpact(companyProfile);
        factors.put("company_size", new PricingFactor(
            "Company Size",
            sizeImpact,
            companyProfile.size() + " company"
        ));
        totalImpact += sizeImpact;
        
        // Industry factor
        double industryImpact = calculateIndustryImpact(companyProfile.industry());
        factors.put("industry", new PricingFactor(
            "Industry",
            industryImpact,
            companyProfile.industry() + " sector"
        ));
        totalImpact += industryImpact;
        
        // Competitive factor
        double competitiveImpact = calculateCompetitiveImpact(marketPricing);
        factors.put("market_position", new PricingFactor(
            "Market Position",
            competitiveImpact,
            "Competitive pricing adjustment"
        ));
        totalImpact += competitiveImpact;
        
        // Calculate final price
        double priceMultiplier = 1.0 + totalImpact;
        priceMultiplier = Math.max(0.7, Math.min(1.5, priceMultiplier)); // Cap at ±30%
        
        BigDecimal personalizedPrice = basePrice.multiply(
            BigDecimal.valueOf(priceMultiplier)
        ).setScale(2, RoundingMode.HALF_UP);
        
        BigDecimal discountPercent = basePrice.subtract(personalizedPrice)
            .divide(basePrice, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        
        // Build result
        PersonalizedPricing pricing = new PersonalizedPricing();
        pricing.organization = org;
        pricing.generatedAt = Instant.now();
        pricing.basePrice = basePrice;
        pricing.personalizedPrice = personalizedPrice;
        pricing.discountPercent = discountPercent;
        pricing.pricingFactors = factors;
        pricing.confidenceScore = 0.85;
        pricing.validUntil = Instant.now().plus(Duration.ofDays(30));
        
        return pricing;
    }
    
    private double calculateCLVImpact(double clv, double currentPrice) {
        // Higher CLV = lower price to retain/upsell
        double clvRatio = clv / (currentPrice * 36); // 36 months
        if (clvRatio > 10) return -0.15; // 15% discount for high value
        if (clvRatio > 5) return -0.10;
        if (clvRatio > 3) return -0.05;
        if (clvRatio < 1) return 0.10; // 10% premium for low value
        return 0.0;
    }
    
    private double calculateUsageImpact(UsageProfile profile) {
        // Heavy users get volume discount
        if (profile.intensityScore() > 0.8) return -0.10;
        if (profile.intensityScore() > 0.6) return -0.05;
        if (profile.intensityScore() < 0.2) return 0.05;
        return 0.0;
    }
    
    private double calculateSizeImpact(CompanyProfile profile) {
        return switch (profile.size()) {
            case ENTERPRISE -> -0.10; // Enterprise discount
            case LARGE -> -0.05;
            case MEDIUM -> 0.0;
            case SMALL -> 0.05;
            case STARTUP -> 0.10; // Startup premium (high churn risk)
        };
    }
    
    private double calculateIndustryImpact(String industry) {
        // Industry-specific pricing
        return switch (industry.toLowerCase()) {
            case "finance", "healthcare" -> 0.10; // Higher willingness to pay
            case "non-profit", "education" -> -0.15; // Discount
            case "technology", "saas" -> -0.05; // Competitive sector
            default -> 0.0;
        };
    }
    
    private double calculateCompetitiveImpact(MarketPricing market) {
        // Price relative to market
        if (market.ourPosition() < 0.8) return 0.05; // We're cheaper, can increase
        if (market.ourPosition() > 1.2) return -0.10; // We're expensive, discount
        return 0.0;
    }
    
    private Uni<UsageProfile> analyzeUsagePatterns(Organization org) {
        return UsageRecord.<UsageRecord>find(
            "organization = ?1 and timestamp >= ?2",
            org,
            Instant.now().minus(30, ChronoUnit.DAYS)
        ).list()
        .map(records -> {
            long totalUsage = records.stream()
                .mapToLong(r -> r.quantity)
                .sum();
            
            double avgDailyUsage = totalUsage / 30.0;
            double intensityScore = Math.min(1.0, avgDailyUsage / 1000.0);
            
            return new UsageProfile(
                totalUsage,
                avgDailyUsage,
                intensityScore,
                records.size()
            );
        });
    }
    
    private Uni<CompanyProfile> assessCompanyProfile(Organization org) {
        return Uni.createFrom().item(() -> {
            CompanySize size = determineCompanySize(org);
            String industry = org.metadata.getOrDefault("industry", "technology").toString();
            
            return new CompanyProfile(size, industry);
        });
    }
    
    private CompanySize determineCompanySize(Organization org) {
        // Determine based on users, revenue, etc.
        if (org.activeSubscription.plan.tier == PlanTier.ENTERPRISE) {
            return CompanySize.ENTERPRISE;
        }
        return CompanySize.MEDIUM;
    }
}