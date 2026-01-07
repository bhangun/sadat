package tech.kayys.wayang.billing.resource;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.billing.domain.UsageAggregate;
import tech.kayys.wayang.billing.dto.RecordUsageRequest;
import tech.kayys.wayang.billing.model.QuotaStatus;
import tech.kayys.wayang.billing.model.UsageType;
import tech.kayys.wayang.billing.service.UsageTrackingService;

@Path("/api/v1/management/usage")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@SecurityRequirement(name = "bearer")
@Tag(name = "Usage", description = "Usage tracking and analytics")
public class UsageResource {
    
    @Inject
    UsageTrackingService usageTrackingService;
    
    /**
     * Record usage event
     */
    @POST
    @Path("/record")
    @Operation(summary = "Record usage event")
    public Uni<RestResponse<Void>> recordUsage(
            @Valid RecordUsageRequest request) {
        
        return usageTrackingService.recordUsage(request)
            .map(v -> RestResponse.accepted());
    }
    
    /**
     * Get current period usage
     */
    @GET
    @Path("/organizations/{organizationId}/current")
    @Operation(summary = "Get current period usage")
    public Uni<UsageAggregate> getCurrentUsage(
            @PathParam("organizationId") UUID organizationId) {
        
        return usageTrackingService.getCurrentPeriodUsage(
            organizationId, 
            YearMonth.now()
        );
    }
    
    /**
     * Get usage history
     */
    @GET
    @Path("/organizations/{organizationId}/history")
    @Operation(summary = "Get usage history")
    public Uni<List<UsageAggregate>> getUsageHistory(
            @PathParam("organizationId") UUID organizationId,
            @QueryParam("startMonth") String startMonth,
            @QueryParam("endMonth") String endMonth) {
        
        YearMonth start = YearMonth.parse(startMonth);
        YearMonth end = YearMonth.parse(endMonth);
        
        return usageTrackingService.getUsageHistory(organizationId, start, end);
    }
    
    /**
     * Get usage breakdown
     */
    @GET
    @Path("/organizations/{organizationId}/breakdown")
    @Operation(summary = "Get usage breakdown")
    public Uni<Map<UsageType, Long>> getUsageBreakdown(
            @PathParam("organizationId") UUID organizationId,
            @QueryParam("yearMonth") String yearMonth) {
        
        return usageTrackingService.getUsageBreakdown(
            organizationId, 
            YearMonth.parse(yearMonth)
        );
    }
    
    /**
     * Get quota status
     */
    @GET
    @Path("/organizations/{organizationId}/quotas")
    @Operation(summary = "Get quota status")
    public Uni<Map<String, QuotaStatus>> getQuotaStatus(
            @PathParam("organizationId") UUID organizationId) {
        
        return usageTrackingService.getQuotaStatus(organizationId);
    }
}
