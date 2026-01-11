package tech.kayys.wayang;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.Map;

@Path("/internal/api-keys")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class IntrospectionResource {

    @Inject
    ApiKeyHasher hasher;

    @POST
    @Path("/introspect")
    @Transactional
    public ConsumerContext introspect(Map<String, String> req) {
        String apiKey = req.get("apiKey");
        if (apiKey == null) {
            return ConsumerContext.inactive();
        }

        String hash = hasher.hash(apiKey);
        ApiKey key = ApiKey.find("keyHash", hash).firstResult();

        if (key == null || !key.active) {
            return ConsumerContext.inactive();
        }

        // Check expiration
        if (key.expiresAt != null && Instant.now().isAfter(key.expiresAt)) {
            key.active = false;
            key.persist();
            return ConsumerContext.inactive();
        }

        // Update last used
        key.lastUsedAt = Instant.now();
        key.persist();

        return ConsumerContext.active(key);
    }
}
