package tech.kayys.wayang.workflow.service.backup;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Repository for managing system and tenant configurations
 */
@ApplicationScoped
public class ConfigurationRepository {

    @Inject
    EntityManager entityManager;

    @Inject
    ConfigurationCache configCache;

    /**
     * Find all configurations
     */
    public Uni<List<SystemConfig>> findAll() {
        return Uni.createFrom().item(() -> entityManager.createQuery("FROM SystemConfig", SystemConfig.class)
                .getResultList());
    }

    /**
     * Find configurations modified since a specific time
     */
    public Uni<List<SystemConfig>> findModifiedSince(Instant since) {
        return Uni.createFrom().item(() -> entityManager.createQuery(
                "FROM SystemConfig WHERE lastModified >= :since",
                SystemConfig.class)
                .setParameter("since", since)
                .getResultList());
    }

    /**
     * Find configuration by key and tenant
     */
    public Uni<Optional<SystemConfig>> findByKey(String key, String tenantId) {
        return Uni.createFrom().item(() -> entityManager.createQuery(
                "FROM SystemConfig WHERE configKey = :key AND tenantId = :tenantId",
                SystemConfig.class)
                .setParameter("key", key)
                .setParameter("tenantId", tenantId)
                .getResultStream()
                .findFirst());
    }

    /**
     * Save or update configuration
     */
    public Uni<SystemConfig> save(SystemConfig config) {
        return Uni.createFrom().item(() -> {
            config.setLastModified(Instant.now());

            if (config.getId() == null) {
                config.setId(UUID.randomUUID().toString());
                entityManager.persist(config);
            } else {
                config = entityManager.merge(config);
            }

            configCache.invalidate(config.getConfigKey(), config.getTenantId());
            return config;
        });
    }

    /**
     * Delete configuration
     */
    public Uni<Boolean> delete(String configId) {
        return Uni.createFrom().item(() -> {
            SystemConfig config = entityManager.find(SystemConfig.class, configId);
            if (config != null) {
                entityManager.remove(config);
                configCache.invalidate(config.getConfigKey(), config.getTenantId());
                return true;
            }
            return false;
        });
    }
}