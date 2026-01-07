package tech.kayys.wayang;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Path("/admin/api-keys")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("platform-admin")
public class ApiKeyAdminResource {

    @Inject
    ApiKeyHasher hasher;

    @POST
    @Transactional
    public Map<String, String> create() {
        String raw = UUID.randomUUID().toString();

        ApiKey key = new ApiKey();
        key.keyHash = hasher.hash(raw);
        key.active = true;
        key.scopes = Set.of("workflow.execute");

        key.persist();

        return Map.of("apiKey", raw);
    }
}
