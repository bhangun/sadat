package tech.kayys.wayang.agent.service;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.repository.SecretRepository;

/**
 * Secure secret management
 */
@ApplicationScoped
public class SecretManager {

    private static final Logger LOG = LoggerFactory.getLogger(SecretManager.class);

    @Inject
    SecretRepository secretRepository;

    // In-memory cache for secrets (encrypted at rest)
    private final Map<String, CachedSecret> cache = new ConcurrentHashMap<>();

    /**
     * Get secret value
     */
    public Uni<String> getSecret(String key, String tenantId) {
        String cacheKey = tenantId + ":" + key;

        // Check cache
        CachedSecret cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return Uni.createFrom().item(decrypt(cached.encryptedValue));
        }

        // Load from database
        return secretRepository.findByKey(key, tenantId)
                .map(entity -> {
                    if (entity == null) {
                        LOG.warn("Secret not found: {}", key);
                        return null;
                    }

                    String decrypted = decrypt(entity.getEncryptedValue());

                    // Cache encrypted value
                    cache.put(cacheKey, new CachedSecret(entity.getEncryptedValue()));

                    return decrypted;
                });
    }

    /**
     * Store secret
     */
    public Uni<Void> setSecret(String key, String value, String tenantId) {
        String encrypted = encrypt(value);

        return secretRepository.save(key, encrypted, tenantId)
                .onItem().invoke(v -> {
                    // Update cache
                    String cacheKey = tenantId + ":" + key;
                    cache.put(cacheKey, new CachedSecret(encrypted));
                    LOG.info("Secret stored: {}", key);
                });
    }

    /**
     * Delete secret
     */
    public Uni<Void> deleteSecret(String key, String tenantId) {
        String cacheKey = tenantId + ":" + key;
        cache.remove(cacheKey);

        return secretRepository.delete(key, tenantId);
    }

    /**
     * Encrypt value
     * In production, use proper encryption (AES-256, KMS, etc.)
     */
    private String encrypt(String value) {
        // Placeholder - implement proper encryption
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    /**
     * Decrypt value
     */
    private String decrypt(String encrypted) {
        // Placeholder - implement proper decryption
        return new String(Base64.getDecoder().decode(encrypted));
    }

    private static class CachedSecret {
        final String encryptedValue;
        final long timestamp;

        CachedSecret(String encryptedValue) {
            this.encryptedValue = encryptedValue;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 3600000; // 1 hour
        }
    }
}