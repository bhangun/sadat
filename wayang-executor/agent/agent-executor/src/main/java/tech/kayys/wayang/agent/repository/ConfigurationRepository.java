package tech.kayys.wayang.agent.repository;

import java.util.List;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.model.AgentConfiguration;

/**
 * Repository for agent configurations
 */
public interface ConfigurationRepository {

    Uni<AgentConfiguration> findByAgentId(String agentId, String tenantId);

    Uni<Void> save(AgentConfiguration config);

    Uni<Void> delete(String agentId, String tenantId);

    Uni<List<AgentConfiguration>> findByTenant(String tenantId);
}
