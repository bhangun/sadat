package tech.kayys.wayang.agent.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.model.AgentConfiguration;
import tech.kayys.wayang.agent.repository.ConfigurationRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================================
 * AGENT SUPPORT SERVICES
 * ============================================================================
 * 
 * Supporting services for agent operations:
 * - Configuration management
 * - Context management
 * - Metrics collection
 * - Storage services
 */

// ==================== CONFIGURATION SERVICE ====================

/**
 * Service for managing agent configurations
 */
@ApplicationScoped
public class AgentConfigurationService {

    private static final Logger LOG = LoggerFactory.getLogger(AgentConfigurationService.class);

    private final Map<String, AgentConfiguration> configCache = new ConcurrentHashMap<>();

    @Inject
    ConfigurationRepository configRepository;

    /**
     * Load agent configuration
     */
    public Uni<AgentConfiguration> loadConfiguration(
            String agentId,
            String tenantId) {

        String key = makeKey(agentId, tenantId);

        // Check cache first
        AgentConfiguration cached = configCache.get(key);
        if (cached != null) {
            LOG.debug("Configuration loaded from cache: {}", key);
            return Uni.createFrom().item(cached);
        }

        // Load from repository
        return configRepository.findByAgentId(agentId, tenantId)
                .onItem().invoke(config -> {
                    if (config != null) {
                        configCache.put(key, config);
                        LOG.debug("Configuration loaded from repository: {}", key);
                    }
                })
                .onFailure().invoke(error -> LOG.error("Failed to load configuration: {}", error.getMessage()));
    }

    /**
     * Save agent configuration
     */
    public Uni<Void> saveConfiguration(AgentConfiguration config) {
        String key = makeKey(config.agentId(), config.tenantId());

        return configRepository.save(config)
                .onItem().invoke(v -> {
                    configCache.put(key, config);
                    LOG.info("Configuration saved: {}", key);
                });
    }

    /**
     * Delete agent configuration
     */
    public Uni<Void> deleteConfiguration(String agentId, String tenantId) {
        String key = makeKey(agentId, tenantId);

        return configRepository.delete(agentId, tenantId)
                .onItem().invoke(v -> {
                    configCache.remove(key);
                    LOG.info("Configuration deleted: {}", key);
                });
    }

    private String makeKey(String agentId, String tenantId) {
        return tenantId + ":" + agentId;
    }
}
