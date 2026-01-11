package tech.kayys.wayang;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.dto.ApiKeyInfo;
import tech.kayys.wayang.dto.ApiKeyResponse;
import tech.kayys.wayang.dto.CreateApiKeyRequest;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Path("/admin/api-keys")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
//@RolesAllowed("platform-admin") // Commented out for now as per user request to consolidate/improve
public class ApiKeyAdminResource {

    private static final String KEY_PREFIX = "sk_";

    @Inject
    ApiKeyHasher hasher;

    @POST
    @Transactional
    public ApiKeyResponse create(CreateApiKeyRequest request) {
        String rawKey = generateSecureKey();
        
        ApiKey key = new ApiKey();
        key.keyHash = hasher.hash(rawKey);
        key.keyPrefix = rawKey.substring(0, 8);
        key.name = request.name();
        key.description = request.description();
        key.createdAt = Instant.now();
        key.expiresAt = request.expiresInDays() != null
                ? Instant.now().plusSeconds(request.expiresInDays() * 86400L)
                : null;
        key.scopes = request.scopes() != null ? request.scopes() : Set.of("workflow.execute");
        key.metadata = request.metadata();
        key.active = true;

        key.persist();

        return new ApiKeyResponse(
                key.id,
                rawKey,
                key.keyPrefix,
                key.name,
                key.createdAt,
                key.expiresAt,
                key.scopes
        );
    }

    @GET
    public List<ApiKeyInfo> list() {
        return ApiKey.<ApiKey>find("active", true)
                .list()
                .stream()
                .map(key -> new ApiKeyInfo(
                        key.id,
                        key.keyPrefix + "...",
                        key.name,
                        key.createdAt,
                        key.expiresAt,
                        key.lastUsedAt,
                        key.scopes
                ))
                .toList();
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public void delete(@PathParam("id") Long id) {
        ApiKey key = ApiKey.findById(id);
        if (key != null) {
            key.active = false;
            key.persist();
        }
    }

    private String generateSecureKey() {
        byte[] randomBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(randomBytes);
        return KEY_PREFIX + Base64.getEncoder()
                .encodeToString(randomBytes)
                .replace("+", "")
                .replace("/", "")
                .replace("=", "")
                .substring(0, 40);
    }
}
