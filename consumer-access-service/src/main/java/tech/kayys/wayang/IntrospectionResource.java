package tech.kayys.wayang;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;

@Path("/internal/api-keys")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IntrospectionResource {

    @Inject
    ApiKeyHasher hasher;

    @POST
    @Path("/introspect")
    public ConsumerContext introspect(Map<String, String> req) {
        String hash = hasher.hash(req.get("apiKey"));
        ApiKey key = ApiKey.find("keyHash", hash).firstResult();

        if (key == null || !key.active) {
            return ConsumerContext.inactive();
        }

        return ConsumerContext.active(key);
    }
}
