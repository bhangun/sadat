package tech.kayys.wayang.billing.domain;


import jakarta.persistence.*;
import tech.kayys.wayang.billing.model.PricingFactor;
import tech.kayys.wayang.organization.domain.Organization;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * ============================================================================
 * DYNAMIC PRICING ENGINE
 * ============================================================================
 * 
 * Personalized pricing based on:
 * - Customer lifetime value (CLV)
 * - Usage patterns
 * - Company size/industry
 * - Competitive intelligence
 * - Willingness to pay indicators
 */

@Entity
@Table(name = "pricing_personalization")
public class PersonalizedPricing {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID pricingId;
    
    @ManyToOne
    @JoinColumn(name = "organization_id")
    public Organization organization;
    
    @Column(name = "generated_at")
    public Instant generatedAt;
    
    @Column(name = "base_price", precision = 19, scale = 4)
    public BigDecimal basePrice;
    
    @Column(name = "personalized_price", precision = 19, scale = 4)
    public BigDecimal personalizedPrice;
    
    @Column(name = "discount_percent", precision = 5, scale = 2)
    public BigDecimal discountPercent;
    
    @Column(name = "pricing_factors", columnDefinition = "jsonb")
    public Map<String, PricingFactor> pricingFactors;
    
    @Column(name = "confidence_score")
    public double confidenceScore;
    
    @Column(name = "valid_until")
    public Instant validUntil;
    
    @Column(name = "accepted")
    public Boolean accepted;
}

