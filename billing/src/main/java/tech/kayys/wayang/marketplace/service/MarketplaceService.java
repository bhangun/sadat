package tech.kayys.wayang.marketplace.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.silat.model.WorkflowDefinition;
import tech.kayys.wayang.billing.dto.PublishWorkflowRequest;
import tech.kayys.wayang.billing.dto.RevenueShare;
import tech.kayys.wayang.billing.dto.SortBy;
import tech.kayys.wayang.billing.model.ListingStatus;
import tech.kayys.wayang.billing.model.PricingModel;
import tech.kayys.wayang.billing.model.WorkflowCategory;
import tech.kayys.wayang.billing.service.RevenueShareCalculator;
import tech.kayys.wayang.billing.service.WorkflowInstaller;
import tech.kayys.wayang.marketplace.domain.MarketplaceInstallation;
import tech.kayys.wayang.marketplace.domain.MarketplaceListing;
import tech.kayys.wayang.marketplace.domain.MarketplaceReview;
import tech.kayys.wayang.marketplace.model.InstallationStatus;
import tech.kayys.wayang.organization.domain.Organization;

@ApplicationScoped
public class MarketplaceService {
    
    private static final Logger LOG = LoggerFactory.getLogger(MarketplaceService.class);
    
    @Inject
    WorkflowInstaller workflowInstaller;
    
    @Inject
    RevenueShareCalculator revenueCalculator;
    
    /**
     * Publish workflow to marketplace
     */
    public Uni<MarketplaceListing> publishWorkflow(
            String workflowId,
            UUID publisherOrgId,
            PublishWorkflowRequest request) {
        
        LOG.info("Publishing workflow to marketplace: {}", workflowId);
        
        return Organization.<Organization>findById(publisherOrgId)
            .flatMap(publisher -> 
                // Get workflow definition
                getWorkflowDefinition(workflowId)
                    .flatMap(definition -> 
                        createListing(publisher, definition, request)
                    )
            );
    }
    
    /**
     * Create marketplace listing
     */
    private Uni<MarketplaceListing> createListing(
            Organization publisher,
            WorkflowDefinition workflowDefinition,
            PublishWorkflowRequest request) {
        
        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() -> {
            MarketplaceListing listing = new MarketplaceListing();
            listing.publisher = publisher;
            listing.publisherName = publisher.name;
            listing.workflowDefinitionId = workflowDefinition.id().value();
            listing.name = request.name();
            listing.tagline = request.tagline();
            listing.description = request.description();
            listing.category = request.category();
            listing.tags = request.tags();
            listing.pricingModel = request.pricingModel();
            listing.price = request.price();
            listing.currency = request.currency();
            listing.iconUrl = request.iconUrl();
            listing.screenshots = request.screenshots();
            listing.demoVideoUrl = request.demoVideoUrl();
            listing.workflowTemplate = workflowDefinition;
            listing.requirements = request.requirements();
            listing.compatiblePlans = request.compatiblePlans();
            listing.currentVersion = "1.0.0";
            listing.status = ListingStatus.PENDING_REVIEW;
            listing.createdAt = Instant.now();
            listing.updatedAt = Instant.now();
            
            return listing.persist()
                .map(v -> listing);
        });
    }
    
    /**
     * Browse marketplace
     */
    public Uni<List<MarketplaceListing>> browseMarketplace(
            WorkflowCategory category,
            PricingModel pricingModel,
            String searchQuery,
            SortBy sortBy,
            int page,
            int size) {
        
        StringBuilder query = new StringBuilder("status = ?1");
        List<Object> params = new ArrayList<>();
        params.add(ListingStatus.PUBLISHED);
        
        if (category != null) {
            query.append(" and category = ?").append(params.size() + 1);
            params.add(category);
        }
        
        if (pricingModel != null) {
            query.append(" and pricingModel = ?").append(params.size() + 1);
            params.add(pricingModel);
        }
        
        if (searchQuery != null && !searchQuery.isBlank()) {
            query.append(" and (lower(name) like ?").append(params.size() + 1);
            query.append(" or lower(description) like ?").append(params.size() + 1).append(")");
            String searchPattern = "%" + searchQuery.toLowerCase() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }
        
        query.append(" order by ");
        query.append(switch (sortBy) {
            case POPULAR -> "installCount desc";
            case RATING -> "averageRating desc";
            case NEWEST -> "publishedAt desc";
            case PRICE_LOW -> "price asc";
            case PRICE_HIGH -> "price desc";
        });
        
        return MarketplaceListing.find(query.toString(), params.toArray())
            .page(page, size)
            .list();
    }
    
    /**
     * Install workflow from marketplace
     */
    public Uni<MarketplaceInstallation> installWorkflow(
            UUID listingId,
            UUID organizationId,
            Map<String, Object> configuration) {
        
        LOG.info("Installing marketplace workflow: {} for org: {}", 
            listingId, organizationId);
        
        return Uni.combine().all()
            .unis(
                MarketplaceListing.<MarketplaceListing>findById(listingId),
                Organization.<Organization>findById(organizationId)
            )
            .asTuple()
            .flatMap(tuple -> {
                MarketplaceListing listing = tuple.getItem1();
                Organization org = tuple.getItem2();
                
                if (listing == null || org == null) {
                    return Uni.createFrom().failure(
                        new NoSuchElementException("Listing or organization not found"));
                }
                
                // Check compatibility
                return checkCompatibility(listing, org)
                    .flatMap(compatible -> {
                        if (!compatible) {
                            return Uni.createFrom().failure(
                                new IllegalStateException("Workflow not compatible with plan"));
                        }
                        
                        // Process payment if needed
                        return processPayment(listing, org)
                            .flatMap(paid -> 
                                workflowInstaller.install(listing, org, configuration)
                            )
                            .flatMap(installation ->
                                recordInstallation(listing, org, installation, configuration)
                            );
                    });
            });
    }
    
    /**
     * Check compatibility
     */
    private Uni<Boolean> checkCompatibility(
            MarketplaceListing listing,
            Organization org) {
        
        if (listing.requirements == null) {
            return Uni.createFrom().item(true);
        }
        
        // Check plan tier
        if (org.activeSubscription != null) {
            int orgTier = org.activeSubscription.plan.tier.ordinal();
            int minTier = listing.requirements.minPlanTier;
            
            if (orgTier < minTier) {
                return Uni.createFrom().item(false);
            }
        }
        
        // TODO: Check other requirements
        
        return Uni.createFrom().item(true);
    }
    
    /**
     * Process payment for workflow
     */
    private Uni<Boolean> processPayment(
            MarketplaceListing listing,
            Organization org) {
        
        if (listing.pricingModel == PricingModel.FREE) {
            return Uni.createFrom().item(true);
        }
        
        // TODO: Integrate with payment processor
        LOG.info("Processing payment: {} {} for listing: {}", 
            listing.price, listing.currency, listing.listingId);
        
        return Uni.createFrom().item(true);
    }
    
    /**
     * Record installation
     */
    private Uni<MarketplaceInstallation> recordInstallation(
            MarketplaceListing listing,
            Organization org,
            String workflowInstanceId,
            Map<String, Object> configuration) {
        
        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() -> {
            MarketplaceInstallation installation = new MarketplaceInstallation();
            installation.listing = listing;
            installation.organization = org;
            installation.installedVersion = listing.currentVersion;
            installation.workflowInstanceId = workflowInstanceId;
            installation.status = InstallationStatus.ACTIVE;
            installation.installedAt = Instant.now();
            installation.lastUpdated = Instant.now();
            installation.configuration = configuration;
            
            return installation.persist()
                .flatMap(v -> {
                    // Update listing stats
                    listing.installCount++;
                    listing.activeInstallations++;
                    return listing.persist()
                        .replaceWith(installation);
                });
        });
    }
    
    /**
     * Submit review
     */
    public Uni<MarketplaceReview> submitReview(
            UUID listingId,
            UUID reviewerOrgId,
            int rating,
            String title,
            String comment) {
        
        return Uni.combine().all()
            .unis(
                MarketplaceListing.<MarketplaceListing>findById(listingId),
                Organization.<Organization>findById(reviewerOrgId),
                checkVerifiedPurchase(listingId, reviewerOrgId)
            )
            .asTuple()
            .flatMap(tuple -> {
                MarketplaceListing listing = tuple.getItem1();
                Organization reviewer = tuple.getItem2();
                boolean verified = tuple.getItem3();
                
                return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() -> {
                    MarketplaceReview review = new MarketplaceReview();
                    review.listing = listing;
                    review.reviewer = reviewer;
                    review.rating = rating;
                    review.title = title;
                    review.comment = comment;
                    review.createdAt = Instant.now();
                    review.verifiedPurchase = verified;
                    
                    return review.persist()
                        .flatMap(v -> updateListingRating(listing))
                        .replaceWith(review);
                });
            });
    }
    
    /**
     * Update listing rating
     */
    private Uni<Void> updateListingRating(MarketplaceListing listing) {
        return MarketplaceReview.<MarketplaceReview>find(
            "listing = ?1", listing
        ).list()
        .map(reviews -> {
            if (reviews.isEmpty()) return null;
            
            double avgRating = reviews.stream()
                .mapToInt(r -> r.rating)
                .average()
                .orElse(0.0);
            
            listing.averageRating = BigDecimal.valueOf(avgRating)
                .setScale(2, java.math.RoundingMode.HALF_UP);
            listing.reviewCount = reviews.size();
            
            return null;
        })
        .flatMap(v -> listing.persist().replaceWithVoid());
    }
    
    /**
     * Check if organization has purchased workflow
     */
    private Uni<Boolean> checkVerifiedPurchase(UUID listingId, UUID orgId) {
        return MarketplaceInstallation.count(
            "listing.listingId = ?1 and organization.organizationId = ?2",
            listingId, orgId
        ).map(count -> count > 0);
    }
    
    /**
     * Calculate revenue share for publisher
     */
    public Uni<RevenueShare> calculateRevenueShare(
            UUID publisherOrgId,
            java.time.YearMonth month) {
        
        return revenueCalculator.calculateMonthlyRevenue(publisherOrgId, month);
    }
    
    private Uni<WorkflowDefinition> getWorkflowDefinition(String workflowId) {
        // TODO: Fetch from workflow registry
        return Uni.createFrom().nullItem();
    }
}

