package tech.kayys.wayang.billing.client;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import tech.kayys.wayang.organization.domain.Organization;

import java.util.Map;

@RegisterRestClient(configKey = "consumer-access-api")
@Path("/internal/consumers")
public interface ConsumerAccessClient {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> register(Map<String, Object> consumer);

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> get(@PathParam("id") String id);

    @GET
    @Path("/tenant/{tenantId}")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, Object> getByTenant(@PathParam("tenantId") String tenantId);

    @GET
    @Path("/{id}/sensitive/decrypted")
    @Produces(MediaType.APPLICATION_JSON)
    Map<String, String> getSensitiveDecrypted(@PathParam("id") String id);
}
