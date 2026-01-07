package tech.kayys.wayang.marketplace.domain;


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
import jakarta.persistence.Table;
import tech.kayys.silat.model.WorkflowDefinition;
import tech.kayys.wayang.billing.model.ListingStatus;
import tech.kayys.wayang.billing.model.PlanTier;
import tech.kayys.wayang.billing.model.PricingModel;
import tech.kayys.wayang.billing.model.Requirements;
import tech.kayys.wayang.billing.model.WorkflowCategory;
import tech.kayys.wayang.organization.domain.Organization;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


/**
 * ============================================================================
 * SILAT WORKFLOW MARKETPLACE
 * ============================================================================
 * 
 * Monetizable marketplace for workflows:
 * - Publish workflows as products
 * - Browse and purchase workflows
 * - Revenue sharing for creators
 * - Ratings and reviews
 * - Auto-updates for installed workflows
 */


@Entity
@Table(name = "marketplace_listings", indexes = {
    @Index(name = "idx_listing_status", columnList = "status"),
    @Index(name = "idx_listing_category", columnList = "category"),
    @Index(name = "idx_listing_rating", columnList = "average_rating")
})
public class MarketplaceListing extends PanacheEntityBase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "listing_id")
    public UUID listingId;
    
    /**
     * Publisher (creator) information
     */
    @ManyToOne
    @JoinColumn(name = "publisher_org_id")
    public Organization publisher;
    
    @Column(name = "publisher_name")
    public String publisherName;
    
    /**
     * Workflow details
     */
    @Column(name = "workflow_definition_id")
    public String workflowDefinitionId;
    
    @Column(name = "name")
    public String name;
    
    @Column(name = "tagline")
    public String tagline;
    
    @Column(name = "description", columnDefinition = "text")
    public String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    public WorkflowCategory category;
    
    @Column(name = "tags", columnDefinition = "jsonb")
    public List<String> tags;
    
    /**
     * Pricing
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_model")
    public PricingModel pricingModel;
    
    @Column(name = "price", precision = 19, scale = 4)
    public BigDecimal price;
    
    @Column(name = "currency", length = 3)
    public String currency = "USD";
    
    @Column(name = "revenue_share_percent", precision = 5, scale = 2)
    public BigDecimal revenueSharePercent = BigDecimal.valueOf(70); // 70% to creator
    
    /**
     * Media
     */
    @Column(name = "icon_url")
    public String iconUrl;
    
    @Column(name = "screenshots", columnDefinition = "jsonb")
    public List<String> screenshots;
    
    @Column(name = "demo_video_url")
    public String demoVideoUrl;
    
    /**
     * Workflow template
     */
    @Column(name = "workflow_template", columnDefinition = "jsonb")
    public WorkflowDefinition workflowTemplate;
    
    /**
     * Requirements & compatibility
     */
    @Column(name = "requirements", columnDefinition = "jsonb")
    public Requirements requirements;
    
    @Column(name = "compatible_plans", columnDefinition = "jsonb")
    public List<PlanTier> compatiblePlans;
    
    /**
     * Statistics
     */
    @Column(name = "install_count")
    public long installCount = 0;
    
    @Column(name = "active_installations")
    public long activeInstallations = 0;
    
    @Column(name = "total_revenue", precision = 19, scale = 4)
    public BigDecimal totalRevenue = BigDecimal.ZERO;
    
    @Column(name = "average_rating", precision = 3, scale = 2)
    public BigDecimal averageRating;
    
    @Column(name = "review_count")
    public long reviewCount = 0;
    
    /**
     * Status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    public ListingStatus status = ListingStatus.DRAFT;
    
    @Column(name = "published_at")
    public Instant publishedAt;
    
    @Column(name = "created_at")
    public Instant createdAt;
    
    @Column(name = "updated_at")
    public Instant updatedAt;
    
    /**
     * Versions
     */
    @Column(name = "current_version")
    public String currentVersion;
    
    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL)
    public List<ListingVersion> versions = new ArrayList<>();
    
    /**
     * Featured status
     */
    @Column(name = "featured")
    public boolean featured = false;
    
    @Column(name = "featured_until")
    public Instant featuredUntil;
}

