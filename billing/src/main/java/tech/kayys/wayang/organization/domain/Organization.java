package tech.kayys.wayang.organization.domain;


import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import tech.kayys.wayang.billing.domain.Address;
import tech.kayys.wayang.billing.domain.ResourceAllocation;
import tech.kayys.wayang.billing.domain.UsageRecord;
import tech.kayys.wayang.organization.model.OrganizationStatus;
import tech.kayys.wayang.organization.model.OrganizationType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tech.kayys.wayang.subscription.domain.Subscription;


/**
 * ============================================================================
 * SILAT MANAGEMENT & BILLING PLATFORM - DOMAIN MODEL
 * ============================================================================
 * 
 * Comprehensive multi-tenant management system with:
 * - Tenant lifecycle management
 * - Subscription & billing
 * - Usage tracking & quotas
 * - Resource provisioning
 * - Administrative controls
 * - Audit & compliance
 * 
 * Package: tech.kayys.silat.management
 */

// ==================== TENANT MANAGEMENT ====================

/**
 * Organization - Top-level tenant entity
 * Represents a customer organization using the Silat platform
 */
@Entity
@Table(name = "mgmt_organizations", indexes = {
    @Index(name = "idx_org_slug", columnList = "slug", unique = true),
    @Index(name = "idx_org_status", columnList = "status"),
    @Index(name = "idx_org_created", columnList = "created_at")
})
public class Organization extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "organization_id")
    public UUID organizationId;
    
    /**
     * Unique tenant identifier used across the platform
     */
    @NotNull
    @Column(name = "tenant_id", unique = true, length = 64)
    public String tenantId;
    
    /**
     * URL-friendly identifier
     */
    @NotNull
    @Pattern(regexp = "^[a-z0-9-]+$")
    @Column(name = "slug", unique = true, length = 64)
    public String slug;
    
    /**
     * Organization name
     */
    @NotNull
    @Size(min = 2, max = 255)
    @Column(name = "name")
    public String name;
    
    /**
     * Legal business name
     */
    @Column(name = "legal_name")
    public String legalName;
    
    /**
     * Tax/VAT identification number
     */
    @Column(name = "tax_id", length = 50)
    public String taxId;
    
    /**
     * Organization type
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "org_type")
    public OrganizationType orgType = OrganizationType.BUSINESS;
    
    /**
     * Current status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    public OrganizationStatus status = OrganizationStatus.ACTIVE;
    
    /**
     * Billing contact email
     */
    @Email
    @Column(name = "billing_email")
    public String billingEmail;
    
    /**
     * Technical contact email
     */
    @Email
    @Column(name = "technical_email")
    public String technicalEmail;
    
    /**
     * Billing address
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "line1", column = @Column(name = "billing_address_line1")),
        @AttributeOverride(name = "line2", column = @Column(name = "billing_address_line2")),
        @AttributeOverride(name = "city", column = @Column(name = "billing_city")),
        @AttributeOverride(name = "state", column = @Column(name = "billing_state")),
        @AttributeOverride(name = "postalCode", column = @Column(name = "billing_postal_code")),
        @AttributeOverride(name = "country", column = @Column(name = "billing_country"))
    })
    public Address billingAddress;
    
    /**
     * Current active subscription
     */
    @OneToOne(mappedBy = "organization")
    public Subscription activeSubscription;
    
    /**
     * Organization settings
     */
    @Column(name = "settings", columnDefinition = "jsonb")
    public OrganizationSettings settings = new OrganizationSettings();
    
    /**
     * Feature flags
     */
    @Column(name = "feature_flags", columnDefinition = "jsonb")
    public Map<String, Boolean> featureFlags = new HashMap<>();
    
    /**
     * Custom metadata
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
    
    @Column(name = "suspended_at")
    public Instant suspendedAt;
    
    @Column(name = "deleted_at")
    public Instant deletedAt;
    
    /**
     * Users in this organization
     */
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    public List<OrganizationUser> users = new ArrayList<>();
    
    /**
     * Resource allocations
     */
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    public List<ResourceAllocation> resourceAllocations = new ArrayList<>();
    
    /**
     * Usage records
     */
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    public List<UsageRecord> usageRecords = new ArrayList<>();
    
    // Business methods
    
    public boolean isActive() {
        return status == OrganizationStatus.ACTIVE;
    }
    
    public boolean isSuspended() {
        return status == OrganizationStatus.SUSPENDED;
    }
    
    public boolean hasFeature(String feature) {
        return featureFlags.getOrDefault(feature, false);
    }
    
    public void suspend(String reason) {
        this.status = OrganizationStatus.SUSPENDED;
        this.suspendedAt = Instant.now();
        this.metadata.put("suspension_reason", reason);
        this.updatedAt = Instant.now();
    }
    
    public void activate() {
        this.status = OrganizationStatus.ACTIVE;
        this.activatedAt = Instant.now();
        this.suspendedAt = null;
        this.updatedAt = Instant.now();
    }
    
    public void softDelete() {
        this.status = OrganizationStatus.DELETED;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}

