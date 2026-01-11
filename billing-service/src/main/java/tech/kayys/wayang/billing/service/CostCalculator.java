package tech.kayys.wayang.billing.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.billing.domain.UsageAggregate;
import tech.kayys.wayang.billing.model.PricingBracket;
import tech.kayys.wayang.billing.model.PricingTier;
import tech.kayys.wayang.billing.model.UsageType;
import tech.kayys.wayang.organization.domain.Organization;

@ApplicationScoped
public class CostCalculator {
    
    private static final Logger LOG = LoggerFactory.getLogger(CostCalculator.class);
    
    // Pricing tiers (can be loaded from database)
    private static final Map<UsageType, PricingTier> PRICING_TIERS = Map.of(
        UsageType.WORKFLOW_EXECUTION, new PricingTier(
            BigDecimal.valueOf(0.01),  // $0.01 per execution
            List.of(
                new PricingBracket(0, 1000, BigDecimal.valueOf(0.01)),
                new PricingBracket(1001, 10000, BigDecimal.valueOf(0.008)),
                new PricingBracket(10001, Long.MAX_VALUE, BigDecimal.valueOf(0.005))
            )
        ),
        UsageType.AI_TOKEN_USAGE, new PricingTier(
            BigDecimal.valueOf(0.00002),  // $0.00002 per token
            List.of(
                new PricingBracket(0, 100000, BigDecimal.valueOf(0.00002)),
                new PricingBracket(100001, 1000000, BigDecimal.valueOf(0.000015)),
                new PricingBracket(1000001, Long.MAX_VALUE, BigDecimal.valueOf(0.00001))
            )
        ),
        UsageType.API_CALL, new PricingTier(
            BigDecimal.valueOf(0.001),  // $0.001 per API call
            List.of()
        ),
        UsageType.STORAGE_GB_HOUR, new PricingTier(
            BigDecimal.valueOf(0.00014),  // $0.10/GB/month = $0.00014/GB/hour
            List.of()
        )
    );
    
    /**
     * Get unit price for usage type (considering tiered pricing)
     */
    @CacheResult(cacheName = "pricing")
    public BigDecimal getUnitPrice(Organization org, UsageType usageType) {
        LOG.debug("Get unit price for org: {} usageType: {}", org.organizationId, usageType);
        PricingTier tier = PRICING_TIERS.get(usageType);
        if (tier == null) {
            return BigDecimal.ZERO;
        }
        
        // Check if organization has custom pricing
        if (org.activeSubscription != null && 
            org.activeSubscription.metadata != null &&
            org.activeSubscription.metadata.containsKey("customPricing")) {
            // For now, return base price. In real implementation, load custom pricing from metadata
            LOG.debug("Custom pricing detected for org: {}", org.organizationId);
            return tier.basePrice();
        }
        
        return tier.basePrice();
    }
    
    /**
     * Calculate cost with tiered pricing
     */
    public BigDecimal calculateCost(
            Organization org,
            UsageType usageType,
            long quantity,
            long previousUsage) {
        
        PricingTier tier = PRICING_TIERS.get(usageType);
        if (tier == null) {
            return BigDecimal.ZERO;
        }
        
        // If no brackets, use base price
        if (tier.brackets().isEmpty()) {
            return tier.basePrice().multiply(BigDecimal.valueOf(quantity));
        }
        
        BigDecimal totalCost = BigDecimal.ZERO;
        long remainingQuantity = quantity;
        long totalUsage = previousUsage + quantity;
        
        for (PricingBracket bracket : tier.brackets()) {
            if (remainingQuantity <= 0) {
                break;
            }
            
            // Calculate how much of the remaining quantity falls into this bracket
            long bracketStart = Math.max(bracket.from(), previousUsage);
            long bracketEnd = Math.min(bracket.to(), totalUsage);
            
            if (bracketEnd > bracketStart) {
                long quantityInBracket = Math.min(bracketEnd - bracketStart, remainingQuantity);
                BigDecimal bracketCost = bracket.price().multiply(
                    BigDecimal.valueOf(quantityInBracket));
                totalCost = totalCost.add(bracketCost);
                
                remainingQuantity -= quantityInBracket;
                previousUsage += quantityInBracket;
            }
        }
        
        // If there's remaining quantity beyond the last bracket, use the last bracket's price
        if (remainingQuantity > 0 && !tier.brackets().isEmpty()) {
            PricingBracket lastBracket = tier.brackets().get(tier.brackets().size() - 1);
            BigDecimal remainingCost = lastBracket.price().multiply(
                BigDecimal.valueOf(remainingQuantity));
            totalCost = totalCost.add(remainingCost);
        }
        
        return totalCost;
    }
    
    /**
     * Calculate monthly cost estimate
     */
    public Uni<BigDecimal> estimateMonthlyCost(Organization org) {
        if (org.activeSubscription == null) {
            return Uni.createFrom().item(BigDecimal.ZERO);
        }
        
        // Base subscription cost
        BigDecimal baseCost = org.activeSubscription.basePrice != null ? 
            org.activeSubscription.basePrice : BigDecimal.ZERO;
        
        // Add addon costs
        if (org.activeSubscription.addons != null) {
            for (var addon : org.activeSubscription.addons) {
                if (addon.isActive && addon.price != null) {
                    baseCost = baseCost.add(addon.price.multiply(
                        BigDecimal.valueOf(addon.quantity)));
                }
            }
        }
        
        final BigDecimal finalBaseCost = baseCost;
        
        // Get current month usage and project
        return UsageAggregate.<UsageAggregate>find(
            "organization = ?1 and yearMonth = ?2",
            org,
            YearMonth.now()
        ).firstResult()
        .map(aggregate -> {
            BigDecimal estimatedCost = finalBaseCost;
            
            if (aggregate != null && aggregate.totalCost != null) {
                // Add current usage costs
                estimatedCost = estimatedCost.add(aggregate.totalCost);
                
                // Project for full month if not complete
                int daysInMonth = YearMonth.now().lengthOfMonth();
                int currentDay = Instant.now().atZone(java.time.ZoneOffset.UTC)
                    .getDayOfMonth();
                
                if (currentDay < daysInMonth) {
                    // Calculate projection factor (remaining days / elapsed days)
                    double remainingDays = daysInMonth - currentDay;
                    double elapsedDays = currentDay;
                    double projectionFactor = remainingDays / elapsedDays;
                    
                    // Project additional usage cost
                    BigDecimal projectedUsageCost = aggregate.totalCost.multiply(
                        BigDecimal.valueOf(projectionFactor));
                    estimatedCost = estimatedCost.add(projectedUsageCost);
                    
                    LOG.debug("Projected monthly cost for org {}: base={}, current={}, projected={}, total={}", 
                        org.organizationId, finalBaseCost, aggregate.totalCost, projectedUsageCost, estimatedCost);
                }
            }
            
            return estimatedCost;
        });
    }
    
    /**
     * Calculate total cost for usage aggregate
     */
    public BigDecimal calculateAggregateCost(UsageAggregate aggregate) {
        if (aggregate == null || aggregate.usageByType == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal totalCost = BigDecimal.ZERO;
        for (Map.Entry<UsageType, Long> entry : aggregate.usageByType.entrySet()) {
            UsageType usageType = entry.getKey();
            long quantity = entry.getValue();
            
            BigDecimal cost = calculateCost(aggregate.organization, usageType, quantity, 0);
            totalCost = totalCost.add(cost);
        }
        
        return totalCost;
    }
    
    /**
     * Get pricing tier for usage type
     */
    public PricingTier getPricingTier(UsageType usageType) {
        return PRICING_TIERS.get(usageType);
    }
    
    /**
     * Check if usage type has tiered pricing
     */
    public boolean hasTieredPricing(UsageType usageType) {
        PricingTier tier = PRICING_TIERS.get(usageType);
        return tier != null && !tier.brackets().isEmpty();
    }
    
    /**
     * Get effective price for quantity (considering tiers)
     */
    public BigDecimal getEffectivePrice(UsageType usageType, long totalQuantity) {
        PricingTier tier = PRICING_TIERS.get(usageType);
        if (tier == null) {
            return BigDecimal.ZERO;
        }
        
        if (tier.brackets().isEmpty()) {
            return tier.basePrice();
        }
        
        // Find the applicable bracket
        for (PricingBracket bracket : tier.brackets()) {
            if (totalQuantity >= bracket.from() && totalQuantity <= bracket.to()) {
                return bracket.price();
            }
        }
        
        // If beyond last bracket, use last bracket price
        if (!tier.brackets().isEmpty()) {
            return tier.brackets().get(tier.brackets().size() - 1).price();
        }
        
        return tier.basePrice();
    }
}
