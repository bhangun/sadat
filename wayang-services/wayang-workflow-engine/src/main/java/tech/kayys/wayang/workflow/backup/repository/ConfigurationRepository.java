package tech.kayys.wayang.workflow.backup.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.backup.domain.SystemConfig;
import tech.kayys.wayang.workflow.backup.service.ConfigurationCache;

/**
 * Repository for managing system and tenant configurations
 */
@ApplicationScoped
public class ConfigurationRepository implements PanacheRepositoryBase<SystemConfig, String> {

    @Inject
    ConfigurationCache configCache;

    /**
     * Find all configurations
     */
    public Uni<List<SystemConfig>> getAllConfigs() {
        return listAll();
    }

    /**
     * Find configurations modified since a specific time
     */
    public Uni<List<SystemConfig>> findModifiedSince(Instant since) {
        return find("modifiedAt >= ?1", since).list();
    }

    /**
     * Find configuration by key and tenant
     */
    public Uni<Optional<SystemConfig>> findByKey(String key, String tenantId) {
        return find("key = ?1 AND tenantId = ?2", key, tenantId)
                .firstResult()
                .map(Optional::ofNullable);
    }

    /**
     * Save or update configuration
     */
    public Uni<SystemConfig> saveConfig(SystemConfig config) {
        config.setLastModified(Instant.now());

        if (config.getId() == null) {
            config.setId(UUID.randomUUID().toString());
            return persist(config)
                    .onItem().invoke(saved -> configCache.invalidate(saved.getConfigKey(), saved.getTenantId()));
        } else {
            return getSession().onItem().transformToUni(session -> session.merge(config))
                    .onItem().invoke(saved -> configCache.invalidate(saved.getConfigKey(), saved.getTenantId()));
        }
    }

    /**
     * Delete configuration
     */
    public Uni<Boolean> deleteConfig(String configId) {
        return findById(configId)
                .onItem().transformToUni(config -> {
                    if (config != null) {
                        return delete(config)
                                .onItem()
                                .invoke(() -> configCache.invalidate(config.getConfigKey(), config.getTenantId()))
                                .replaceWith(true);
                    }
                    return Uni.createFrom().item(false);
                });
    }
}