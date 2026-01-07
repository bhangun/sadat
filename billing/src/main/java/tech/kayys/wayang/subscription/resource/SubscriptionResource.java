package tech.kayys.wayang.subscription.resource;

import java.util.UUID;
import java.util.concurrent.Flow.Subscription;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.billing.dto.AddAddonRequest;
import tech.kayys.wayang.billing.dto.UpdateSubscriptionRequest;
import tech.kayys.wayang.subscription.domain.SubscriptionAddon;
import tech.kayys.wayang.subscription.dto.CreateSubscriptionRequest;
import tech.kayys.wayang.subscription.service.SubscriptionService;

@Path("/api/v1/management/subscriptions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@SecurityRequirement(name = "bearer")
@Tag(name = "Subscriptions", description = "Subscription management")
public class SubscriptionResource {
    
    @Inject
    SubscriptionService subscriptionService;
    
    /**
     * Create subscription
     */
    @POST
    @Operation(summary = "Create subscription")
    public Uni<RestResponse<Subscription>> createSubscription(
            @Valid CreateSubscriptionRequest request) {
        
        return subscriptionService.createSubscription(request)
            .map(sub -> RestResponse.status(RestResponse.Status.CREATED, sub));
    }
    
    /**
     * Get subscription
     */
    @GET
    @Path("/{subscriptionId}")
    @Operation(summary = "Get subscription")
    public Uni<RestResponse<Subscription>> getSubscription(
            @PathParam("subscriptionId") UUID subscriptionId) {
        
        return subscriptionService.getSubscription(subscriptionId)
            .map(sub -> sub != null ? 
                RestResponse.ok(sub) : 
                RestResponse.notFound());
    }
    
    /**
     * Update subscription (upgrade/downgrade)
     */
    @PUT
    @Path("/{subscriptionId}")
    @Operation(summary = "Update subscription")
    public Uni<RestResponse<Subscription>> updateSubscription(
            @PathParam("subscriptionId") UUID subscriptionId,
            @Valid UpdateSubscriptionRequest request) {
        
        return subscriptionService.updateSubscription(subscriptionId, request)
            .map(RestResponse::ok);
    }
    
    /**
     * Cancel subscription
     */
    @POST
    @Path("/{subscriptionId}/cancel")
    @Operation(summary = "Cancel subscription")
    public Uni<RestResponse<Subscription>> cancelSubscription(
            @PathParam("subscriptionId") UUID subscriptionId,
            @QueryParam("immediate") @DefaultValue("false") boolean immediate,
            @QueryParam("reason") String reason) {
        
        return subscriptionService.cancelSubscription(subscriptionId, immediate, reason)
            .map(RestResponse::ok);
    }
    
    /**
     * Add addon to subscription
     */
    @POST
    @Path("/{subscriptionId}/addons")
    @Operation(summary = "Add subscription addon")
    public Uni<RestResponse<SubscriptionAddon>> addAddon(
            @PathParam("subscriptionId") UUID subscriptionId,
            @Valid AddAddonRequest request) {
        
        return subscriptionService.addAddon(subscriptionId, request)
            .map(RestResponse::ok);
    }
    
    /**
     * Remove addon
     */
    @DELETE
    @Path("/{subscriptionId}/addons/{addonId}")
    @Operation(summary = "Remove subscription addon")
    public Uni<RestResponse<Void>> removeAddon(
            @PathParam("subscriptionId") UUID subscriptionId,
            @PathParam("addonId") UUID addonId) {
        
        return subscriptionService.removeAddon(subscriptionId, addonId)
            .map(v -> RestResponse.noContent());
    }
}