package tech.kayys.wayang.subscription.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import tech.kayys.wayang.billing.domain.ResourceQuotas;
import tech.kayys.wayang.billing.model.PlanTier;

/**
 * Subscription Plan - Pricing tier definition
 */
@Entity
@Table(name = "mgmt_subscription_plans")
public class SubscriptionPlan extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "plan_id")
    public UUID planId;
    
    /**
     * Unique plan identifier
     */
    @NotNull
    @Column(name = "plan_code", unique = true, length = 64)
    public String planCode;
    
    /**
     * Display name
     */
    @NotNull
    @Column(name = "name")
    public String name;
    
    /**
     * Description
     */
    @Column(name = "description", columnDefinition = "text")
    public String description;
    
    /**
     * Plan tier
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tier")
    public PlanTier tier;
    
    /**
     * Pricing
     */
    @Column(name = "monthly_price", precision = 19, scale = 4)
    public BigDecimal monthlyPrice;
    
    @Column(name = "annual_price", precision = 19, scale = 4)
    public BigDecimal annualPrice;
    
    @Column(name = "currency", length = 3)
    public String currency = "USD";
    
    /**
     * Resource quotas
     */
    @Embedded
    public ResourceQuotas quotas = new ResourceQuotas();
    
    /**
     * Features included
     */
    @Column(name = "features", columnDefinition = "jsonb")
    public List<String> features = new ArrayList<>();
    
    /**
     * Plan limits
     */
    @Column(name = "limits", columnDefinition = "jsonb")
    public Map<String, Integer> limits = new HashMap<>();
    
    /**
     * Availability
     */
    @Column(name = "is_active")
    public boolean isActive = true;
    
    @Column(name = "is_public")
    public boolean isPublic = true;
    
    /**
     * Trial configuration
     */
    @Column(name = "trial_days")
    public int trialDays = 14;
    
    @Column(name = "allows_trial")
    public boolean allowsTrial = true;
    
    /**
     * Metadata
     */
    @Column(name = "metadata", columnDefinition = "jsonb")
    public Map<String, Object> metadata = new HashMap<>();
    
    @Column(name = "created_at")
    public Instant createdAt;
    
    @Column(name = "updated_at")
    public Instant updatedAt;
}

