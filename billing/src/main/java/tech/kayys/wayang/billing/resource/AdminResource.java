package tech.kayys.wayang.billing.resource;

import java.time.YearMonth;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.billing.dto.PlatformOverview;
import tech.kayys.wayang.billing.dto.RevenueMetrics;
import tech.kayys.wayang.billing.dto.TenantHealthOverview;
import tech.kayys.wayang.billing.dto.UsageMetrics;
import tech.kayys.wayang.billing.service.AdminDashboardService;
import tech.kayys.wayang.organization.domain.Organization;

@Path("/api/v1/management/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@SecurityRequirement(name = "bearer")
@Tag(name = "Admin", description = "Administrative operations")
public class AdminResource {
    
    @Inject
    AdminDashboardService dashboardService;
    
    /**
     * Get platform overview
     */
    @GET
    @Path("/overview")
    @Operation(summary = "Get platform overview")
    public Uni<PlatformOverview> getPlatformOverview() {
        return dashboardService.getPlatformOverview();
    }
    
    /**
     * Get revenue metrics
     */
    @GET
    @Path("/revenue")
    @Operation(summary = "Get revenue metrics")
    public Uni<RevenueMetrics> getRevenueMetrics(
            @QueryParam("startMonth") String startMonth,
            @QueryParam("endMonth") String endMonth) {
        
        return dashboardService.getRevenueMetrics(
            YearMonth.parse(startMonth),
            YearMonth.parse(endMonth)
        );
    }
    
    /**
     * Get usage metrics
     */
    @GET
    @Path("/metrics/usage")
    @Operation(summary = "Get usage metrics")
    public Uni<UsageMetrics> getUsageMetrics() {
        return dashboardService.getUsageMetrics();
    }
    
    /**
     * Get tenant health
     */
    @GET
    @Path("/health")
    @Operation(summary = "Get tenant health overview")
    public Uni<TenantHealthOverview> getTenantHealth() {
        return dashboardService.getTenantHealthOverview();
    }
    
    /**
     * Search organizations
     */
    @GET
    @Path("/search")
    @Operation(summary = "Search organizations")
    public Uni<List<Organization>> searchOrganizations(
            @QueryParam("query") String query) {
        
        return dashboardService.searchOrganizations(query);
    }
}