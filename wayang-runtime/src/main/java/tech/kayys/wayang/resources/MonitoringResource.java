package tech.kayys.wayang.resources;

import java.time.Instant;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.dto.HealthStatus;
import tech.kayys.wayang.dto.TenantMetrics;
import tech.kayys.wayang.security.service.AuthenticatedUser;
import tech.kayys.wayang.security.service.IketSecurityService;

@Path("/api/v1/monitoring")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Monitoring", description = "System monitoring and metrics")
@SecurityRequirement(name = "bearer-jwt")
public class MonitoringResource {

    @Inject
    IketSecurityService iketSecurity;

    @GET
    @Path("/health")
    @Operation(summary = "Health check")
    public Uni<HealthStatus> getHealth() {
        return Uni.createFrom().item(
                new HealthStatus("UP", Instant.now(), Map.of()));
    }

    @GET
    @Path("/metrics/tenant")
    @Operation(summary = "Tenant metrics")
    @RolesAllowed({ "admin" })
    public Uni<TenantMetrics> getTenantMetrics() {
        AuthenticatedUser user = iketSecurity.getCurrentUser();

        // Gather metrics
        return Uni.createFrom().item(
                new TenantMetrics(
                        user.tenantId(),
                        0L, // Active workflows
                        0L, // Active agents
                        0L, // Total executions today
                        0.0, // Total cost today
                        Instant.now()));
    }
}
