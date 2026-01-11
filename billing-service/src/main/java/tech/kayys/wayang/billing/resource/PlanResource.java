package tech.kayys.wayang.billing.resource;

import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.billing.dto.PlanComparison;
import tech.kayys.wayang.subscription.domain.SubscriptionPlan;

@Path("/api/v1/management/plans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Plans", description = "Subscription plan management")
public class PlanResource {
    
    /**
     * List available plans
     */
    @GET
    @Operation(summary = "List subscription plans")
    public Uni<List<SubscriptionPlan>> listPlans(
            @QueryParam("public") @DefaultValue("true") boolean publicOnly) {
        
        return SubscriptionPlan.<SubscriptionPlan>find(
            publicOnly ? "isActive = true and isPublic = true" : "isActive = true"
        ).list();
    }
    
    /**
     * Get plan details
     */
    @GET
    @Path("/{planId}")
    @Operation(summary = "Get plan details")
    public Uni<RestResponse<SubscriptionPlan>> getPlan(
            @PathParam("planId") UUID planId) {
        
        return SubscriptionPlan.<SubscriptionPlan>findById(planId)
            .map(plan -> plan != null ? 
                RestResponse.ok(plan) : 
                RestResponse.notFound());
    }
    
    /**
     * Compare plans
     */
    @GET
    @Path("/compare")
    @Operation(summary = "Compare subscription plans")
    public Uni<PlanComparison> comparePlans(
            @QueryParam("plans") List<UUID> planIds) {
        
        return SubscriptionPlan.<SubscriptionPlan>list(
            "planId in ?1", planIds
        ).map(PlanComparison::new);
    }
}
