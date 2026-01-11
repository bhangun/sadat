package tech.kayys.wayang.security.service;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import tech.kayys.wayang.security.dto.IntrospectionResponse;

import java.util.Map;

@Path("/internal/api-keys")
@RegisterRestClient(configKey = "consumer-access-api")
public interface ConsumerAccessClient {

    @POST
    @Path("/introspect")
    IntrospectionResponse introspect(Map<String, String> request);
}
