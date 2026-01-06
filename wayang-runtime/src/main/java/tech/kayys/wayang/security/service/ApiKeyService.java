package tech.kayys.wayang.security.service;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.security.domain.ApiKey;
import tech.kayys.wayang.security.dto.ApiKeyInfo;
import tech.kayys.wayang.security.dto.ApiKeyResponse;
import tech.kayys.wayang.security.dto.CreateApiKeyRequest;

/**
 * API Key management service
 */
@ApplicationScoped
public class ApiKeyService {

    private static final Logger LOG = LoggerFactory.getLogger(ApiKeyService.class);
    private static final String KEY_PREFIX = "sk_";

    @Inject
    KeycloakSecurityService keycloakSecurity;

    /**
     * Generate new API key
     */
    public Uni<ApiKeyResponse> generateApiKey(CreateApiKeyRequest request) {
        AuthenticatedUser user = keycloakSecurity.getCurrentUser();
        LOG.info("Generating API key for user {}", user.userId());
        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() -> {
            // Generate secure random key
            String rawKey = generateSecureKey();
            String keyHash = hashKey(rawKey);
            String keyPrefix = rawKey.substring(0, 8);

            ApiKey apiKey = new ApiKey();
            apiKey.tenantId = user.tenantId();
            apiKey.keyHash = keyHash;
            apiKey.keyPrefix = keyPrefix;
            apiKey.name = request.name();
            apiKey.description = request.description();
            apiKey.createdBy = user.userId();
            apiKey.createdAt = Instant.now();
            apiKey.expiresAt = request.expiresInDays() != null
                    ? Instant.now().plusSeconds(request.expiresInDays() * 86400L)
                    : null;
            apiKey.scopes = request.scopes();
            apiKey.rateLimit = request.rateLimit();
            apiKey.metadata = request.metadata();

            return apiKey.persist()
                    .map(saved -> new ApiKeyResponse(
                            ((ApiKey) saved).id,
                            rawKey, // Only returned once!
                            keyPrefix,
                            apiKey.name,
                            apiKey.createdAt,
                            apiKey.expiresAt,
                            apiKey.scopes));
        });
    }

    /**
     * Validate API key
     */
    public Uni<ApiKeyValidation> validateApiKey(String rawKey) {
        String keyHash = hashKey(rawKey);

        return ApiKey.<ApiKey>find("keyHash = ?1 and isActive = true", keyHash)
                .firstResult()
                .flatMap(apiKey -> {
                    if (apiKey == null) {
                        return Uni.createFrom().item(
                                ApiKeyValidation.invalid("Invalid API key"));
                    }

                    // Check expiration
                    if (apiKey.expiresAt != null &&
                            Instant.now().isAfter(apiKey.expiresAt)) {
                        return Uni.createFrom().item(
                                ApiKeyValidation.invalid("API key expired"));
                    }

                    // Update last used
                    apiKey.lastUsedAt = Instant.now();
                    return apiKey.persist()
                            .map(v -> ApiKeyValidation.valid(
                                    apiKey.tenantId,
                                    apiKey.scopes,
                                    apiKey.rateLimit));
                });
    }

    /**
     * Revoke API key
     */
    public Uni<Void> revokeApiKey(UUID keyId) {
        AuthenticatedUser user = keycloakSecurity.getCurrentUser();

        return io.quarkus.hibernate.reactive.panache.Panache.withTransaction(() -> ApiKey.<ApiKey>findById(keyId)
                .flatMap(apiKey -> {
                    if (apiKey == null) {
                        return Uni.createFrom().failure(
                                new IllegalArgumentException("API key not found"));
                    }

                    // Verify tenant ownership
                    if (!apiKey.tenantId.equals(user.tenantId())) {
                        return Uni.createFrom().failure(
                                new SecurityException("Access denied"));
                    }

                    apiKey.isActive = false;
                    return apiKey.persist().replaceWithVoid();
                }));
    }

    /**
     * List API keys for tenant
     */
    public Uni<List<ApiKeyInfo>> listApiKeys() {
        AuthenticatedUser user = keycloakSecurity.getCurrentUser();

        return ApiKey.<ApiKey>find("tenantId = ?1 and isActive = true",
                user.tenantId())
                .list()
                .map(keys -> keys.stream()
                        .map(key -> new ApiKeyInfo(
                                key.id,
                                key.keyPrefix + "...",
                                key.name,
                                key.createdAt,
                                key.expiresAt,
                                key.lastUsedAt,
                                key.scopes))
                        .toList());
    }

    // ==================== HELPER METHODS ====================

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

    private String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash API key", e);
        }
    }
}
