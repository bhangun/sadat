package tech.kayys.wayang.workflow.resource;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.workflow.multiregion.dto.HealthCheckResponse;

// REST Client interface for region health endpoints
@RegisterRestClient(configKey = "region-health-api")
@Path("/api/v1/regions")
public interface RegionHealthEndpoint {

    @GET
    @Path("/{regionId}/health")
    @Produces(MediaType.APPLICATION_JSON)
    Uni<HealthCheckResponse> getRegionHealth(@PathParam("regionId") String regionId);
}
