package tech.kayys.wayang.subscription.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import tech.kayys.wayang.billing.model.BillingCycle;
import tech.kayys.wayang.organization.domain.Organization;
import tech.kayys.wayang.subscription.model.SubscriptionStatus;

/**
 * Subscription - Billing subscription for an organization
 */
@Entity
@Table(name = "mgmt_subscriptions", indexes = {
    @Index(name = "idx_sub_org", columnList = "organization_id"),
    @Index(name = "idx_sub_status", columnList = "status"),
    @Index(name = "idx_sub_period", columnList = "current_period_start, current_period_end")
})
public class Subscription extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "subscription_id")
    public UUID subscriptionId;
    
    /**
     * Organization owning this subscription
     */
    @OneToOne
    @JoinColumn(name = "organization_id")
    public Organization organization;
    
    /**
     * Subscription plan
     */
    @ManyToOne
    @JoinColumn(name = "plan_id")
    public SubscriptionPlan plan;
    
    /**
     * Current status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    public SubscriptionStatus status = SubscriptionStatus.ACTIVE;
    
    /**
     * Billing cycle
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle")
    public BillingCycle billingCycle = BillingCycle.MONTHLY;
    
    /**
     * Current billing period
     */
    @Column(name = "current_period_start")
    public Instant currentPeriodStart;
    
    @Column(name = "current_period_end")
    public Instant currentPeriodEnd;
    
    /**
     * Trial information
     */
    @Column(name = "trial_start")
    public Instant trialStart;
    
    @Column(name = "trial_end")
    public Instant trialEnd;
    
    @Column(name = "is_trial")
    public boolean isTrial = false;
    
    /**
     * Cancellation tracking
     */
    @Column(name = "cancel_at_period_end")
    public boolean cancelAtPeriodEnd = false;
    
    @Column(name = "cancelled_at")
    public Instant cancelledAt;
    
    @Column(name = "cancellation_reason")
    public String cancellationReason;
    
    /**
     * Pricing
     */
    @Column(name = "base_price", precision = 19, scale = 4)
    public BigDecimal basePrice;
    
    @Column(name = "currency", length = 3)
    public String currency = "USD";
    
    @Column(name = "discount_percent", precision = 5, scale = 2)
    public BigDecimal discountPercent = BigDecimal.ZERO;
    
    /**
     * Add-ons purchased
     */
    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL)
    public List<SubscriptionAddon> addons = new ArrayList<>();
    
    /**
     * Payment method reference
     */
    @Column(name = "payment_method_id")
    public String paymentMethodId;
    
    /**
     * External billing system reference
     */
    @Column(name = "external_subscription_id")
    public String externalSubscriptionId;
    
    /**
     * Metadata
     */
    @Column(name = "metadata", columnDefinition = "jsonb")
    public Map<String, Object> metadata = new HashMap<>();
    
    /**
     * Temporal tracking
     */
    @Column(name = "created_at")
    public Instant createdAt;
    
    @Column(name = "updated_at")
    public Instant updatedAt;
    
    @Column(name = "activated_at")
    public Instant activatedAt;
    
    // Business methods
    
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE && 
               Instant.now().isBefore(currentPeriodEnd);
    }
    
    public boolean isInTrial() {
        return isTrial && Instant.now().isBefore(trialEnd);
    }
    
    public BigDecimal calculateTotalPrice() {
        BigDecimal total = basePrice;
        
        // Apply discount
        if (discountPercent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = total.multiply(discountPercent)
                .divide(BigDecimal.valueOf(100));
            total = total.subtract(discount);
        }
        
        // Add addon costs
        for (SubscriptionAddon addon : addons) {
            total = total.add(addon.price);
        }
        
        return total;
    }
    
    public void renewPeriod() {
        this.currentPeriodStart = this.currentPeriodEnd;
        this.currentPeriodEnd = calculateNextPeriodEnd();
        this.updatedAt = Instant.now();
    }
    
    private Instant calculateNextPeriodEnd() {
        return switch (billingCycle) {
            case MONTHLY -> currentPeriodStart.plus(java.time.Duration.ofDays(30));
            case QUARTERLY -> currentPeriodStart.plus(java.time.Duration.ofDays(90));
            case ANNUAL -> currentPeriodStart.plus(java.time.Duration.ofDays(365));
        };
    }
    
    public void cancel(String reason) {
        this.cancelAtPeriodEnd = true;
        this.cancellationReason = reason;
        this.cancelledAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
