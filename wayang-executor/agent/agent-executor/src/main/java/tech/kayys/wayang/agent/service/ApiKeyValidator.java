package tech.kayys.wayang.agent.service;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.dto.ApiKeyValidationResult;
import tech.kayys.wayang.agent.repository.ApiKeyRepository;

/**
 * Validates API keys against database
 */
@ApplicationScoped
public class ApiKeyValidator {

    private static final Logger LOG = LoggerFactory.getLogger(ApiKeyValidator.class);

    @Inject
    ApiKeyRepository apiKeyRepository;

    // Cache for validated keys (with TTL)
    private final Map<String, CachedValidation> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 300000; // 5 minutes

    /**
     * Validate API key
     */
    public Uni<ApiKeyValidationResult> validate(String apiKey, String tenantId) {
        String cacheKey = hashApiKey(apiKey) + ":" + tenantId;

        // Check cache first
        CachedValidation cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            LOG.trace("API key validation cache hit");
            return Uni.createFrom().item(cached.result);
        }

        // Validate against database
        return apiKeyRepository.findByKey(apiKey, tenantId)
                .map(entity -> {
                    if (entity == null) {
                        return ApiKeyValidationResult.invalid();
                    }

                    ApiKeyValidationResult result = new ApiKeyValidationResult(
                            true,
                            entity.isActive(),
                            entity.getUserId(),
                            new ArrayList<>(entity.getRoles()),
                            new ArrayList<>(entity.getPermissions()));

                    // Cache result
                    cache.put(cacheKey, new CachedValidation(result));

                    // Update last used timestamp
                    apiKeyRepository.updateLastUsed(entity.getId())
                            .subscribe().with(
                                    v -> LOG.trace("Updated API key last used"),
                                    error -> LOG.warn("Failed to update API key last used", error));

                    return result;
                })
                .onFailure().recoverWithItem(error -> {
                    LOG.error("API key validation failed", error);
                    return ApiKeyValidationResult.invalid();
                });
    }

    /**
     * Hash API key for cache lookup (don't store raw keys)
     */
    private String hashApiKey(String apiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return apiKey; // Fallback
        }
    }

    private static class CachedValidation {
        final ApiKeyValidationResult result;
        final long timestamp;

        CachedValidation(ApiKeyValidationResult result) {
            this.result = result;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
