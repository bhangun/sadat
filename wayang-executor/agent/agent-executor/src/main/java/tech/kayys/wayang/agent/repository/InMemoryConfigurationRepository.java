package tech.kayys.wayang.agent.repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.AgentConfiguration;

/**
 * In-memory implementation of ConfigurationRepository
 * For production, replace with actual database implementation
 */
@ApplicationScoped
public class InMemoryConfigurationRepository implements ConfigurationRepository {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryConfigurationRepository.class);

    private final Map<String, AgentConfiguration> storage = new ConcurrentHashMap<>();

    @Override
    public Uni<AgentConfiguration> findByAgentId(String agentId, String tenantId) {
        String key = makeKey(agentId, tenantId);
        return Uni.createFrom().item(storage.get(key));
    }

    @Override
    public Uni<Void> save(AgentConfiguration config) {
        String key = makeKey(config.agentId(), config.tenantId());
        storage.put(key, config);
        LOG.debug("Saved configuration: {}", key);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<Void> delete(String agentId, String tenantId) {
        String key = makeKey(agentId, tenantId);
        storage.remove(key);
        LOG.debug("Deleted configuration: {}", key);
        return Uni.createFrom().voidItem();
    }

    @Override
    public Uni<List<AgentConfiguration>> findByTenant(String tenantId) {
        List<AgentConfiguration> result = storage.values().stream()
                .filter(config -> tenantId.equals(config.tenantId()))
                .toList();
        return Uni.createFrom().item(result);
    }

    private String makeKey(String agentId, String tenantId) {
        return tenantId + ":" + agentId;
    }
}
