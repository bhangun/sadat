package tech.kayys.wayang.mcp.security;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Vault secret manager for secure credential storage
 */
@ApplicationScoped
public class VaultSecretManager {

    private static final Logger LOG = LoggerFactory.getLogger(VaultSecretManager.class);

    // In production, integrate with HashiCorp Vault, AWS Secrets Manager, etc.
    private final Map<String, String> inMemoryVault = new java.util.concurrent.ConcurrentHashMap<>();

    public Uni<Void> storeSecret(String path, String secret) {
        LOG.info("Storing secret at path: {}", path);
        inMemoryVault.put(path, secret);
        return Uni.createFrom().voidItem();
    }

    public Uni<String> getSecret(String path) {
        String secret = inMemoryVault.get(path);
        if (secret == null) {
            return Uni.createFrom().failure(
                new RuntimeException("Secret not found: " + path));
        }
        return Uni.createFrom().item(secret);
    }

    public Uni<Void> deleteSecret(String path) {
        inMemoryVault.remove(path);
        return Uni.createFrom().voidItem();
    }
}