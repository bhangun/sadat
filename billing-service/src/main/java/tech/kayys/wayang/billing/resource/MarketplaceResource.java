package tech.kayys.wayang.billing.resource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.billing.dto.ReviewRequest;
import tech.kayys.wayang.billing.dto.SortBy;
import tech.kayys.wayang.billing.model.PricingModel;
import tech.kayys.wayang.billing.model.WorkflowCategory;
import tech.kayys.wayang.marketplace.domain.MarketplaceInstallation;
import tech.kayys.wayang.marketplace.domain.MarketplaceListing;
import tech.kayys.wayang.marketplace.domain.MarketplaceReview;
import tech.kayys.wayang.marketplace.service.MarketplaceService;

@Path("/api/v1/marketplace")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Marketplace", description = "Workflow marketplace operations")
public class MarketplaceResource {
    
    @Inject
    MarketplaceService marketplaceService;
    
    /**
     * Browse marketplace
     */
    @GET
    @Operation(summary = "Browse marketplace")
    public Uni<List<MarketplaceListing>> browse(
            @QueryParam("category") WorkflowCategory category,
            @QueryParam("pricing") PricingModel pricingModel,
            @QueryParam("q") String searchQuery,
            @QueryParam("sort") @DefaultValue("POPULAR") SortBy sortBy,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        
        return marketplaceService.browseMarketplace(
            category, pricingModel, searchQuery, sortBy, page, size);
    }
    
    /**
     * Get listing details
     */
    @GET
    @Path("/{listingId}")
    @Operation(summary = "Get listing details")
    public Uni<org.jboss.resteasy.reactive.RestResponse<MarketplaceListing>> getListing(
            @PathParam("listingId") UUID listingId) {
        
        return MarketplaceListing.<MarketplaceListing>findById(listingId)
            .map(listing -> listing != null ?
                org.jboss.resteasy.reactive.RestResponse.ok(listing) :
                org.jboss.resteasy.reactive.RestResponse.notFound());
    }
    
    /**
     * Install workflow
     */
    @POST
    @Path("/{listingId}/install")
    @Operation(summary = "Install workflow")
    public Uni<org.jboss.resteasy.reactive.RestResponse<MarketplaceInstallation>> install(
            @PathParam("listingId") UUID listingId,
            @QueryParam("organizationId") UUID organizationId,
            Map<String, Object> configuration) {
        
        return marketplaceService.installWorkflow(listingId, organizationId, configuration)
            .map(org.jboss.resteasy.reactive.RestResponse::ok);
    }
    
    /**
     * Submit review
     */
    @POST
    @Path("/{listingId}/reviews")
    @Operation(summary = "Submit review")
    public Uni<org.jboss.resteasy.reactive.RestResponse<MarketplaceReview>> submitReview(
            @PathParam("listingId") UUID listingId,
            @QueryParam("organizationId") UUID organizationId,
            @Valid ReviewRequest request) {
        
        return marketplaceService.submitReview(
            listingId,
            organizationId,
            request.rating(),
            request.title(),
            request.comment()
        ).map(review -> org.jboss.resteasy.reactive.RestResponse.status(
            org.jboss.resteasy.reactive.RestResponse.Status.CREATED, review));
    }
}

