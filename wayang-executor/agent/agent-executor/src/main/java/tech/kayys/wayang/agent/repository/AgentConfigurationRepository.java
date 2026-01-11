package tech.kayys.wayang.agent.repository;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.domain.AgentConfigurationEntity;

/**
 * Repository for Agent Configurations
 */
@ApplicationScoped
public class AgentConfigurationRepository
        implements PanacheRepositoryBase<AgentConfigurationEntity, String> {

    private static final Logger LOG = LoggerFactory.getLogger(AgentConfigurationRepository.class);

    public Uni<AgentConfigurationEntity> findByAgentAndTenant(
            String agentId,
            String tenantId) {
        LOG.info("findByAgentAndTenant: agentId = {}, tenantId = {}", agentId, tenantId);
        return find("agentId = ?1 and tenantId = ?2", agentId, tenantId)
                .firstResult();
    }

    public Uni<List<AgentConfigurationEntity>> findByTenant(String tenantId) {
        return list("tenantId", tenantId);
    }

    public Uni<Boolean> deleteByAgentAndTenant(String agentId, String tenantId) {
        return delete("agentId = ?1 and tenantId = ?2", agentId, tenantId)
                .map(count -> count > 0);
    }

    public Uni<AgentConfigurationEntity> persistOrUpdate(AgentConfigurationEntity entity) {
        return getSession().flatMap(s -> s.merge(entity));
    }
}
