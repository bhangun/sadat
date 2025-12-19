package tech.kayys.wayang.agent.service;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.entity.AgentEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AgentRepository implements PanacheRepository<AgentEntity> {

    public Uni<List<AgentEntity>> findByTenantId(String tenantId) {
        Log.debugf("Finding agents for tenant: %s", tenantId);
        return find("tenantId", tenantId).list();
    }

    public Uni<Optional<AgentEntity>> findById(String id) {
        Log.debugf("Finding agent by ID: %s", id);
        try {
            UUID uuid = UUID.fromString(id);
            return findByIdOptional(uuid);
        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid UUID format: %s", id);
            return Uni.createFrom().optional(Optional.empty());
        }
    }

    public Uni<List<AgentEntity>> findByTenantIdAndIsActive(String tenantId, Boolean isActive) {
        Log.debugf("Finding agents for tenant: %s with active status: %s", tenantId, isActive);
        return find("tenantId = ?1 AND isActive = ?2", tenantId, isActive).list();
    }

    public Uni<AgentEntity> findOrchestratorById(String orchestratorId) {
        Log.debugf("Finding orchestrator by ID: %s", orchestratorId);
        try {
            UUID uuid = UUID.fromString(orchestratorId);
            return findById(uuid);
        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid UUID format: %s", orchestratorId);
            return Uni.createFrom().nullItem();
        }
    }

    public Uni<AgentEntity> save(AgentEntity agent) {
        Log.debugf("Saving agent: %s", agent.getName());
        if (agent.getId() == null) {
            return persist(agent);
        } else {
            return persistAndFlush(agent);
        }
    }

    public Uni<Long> updateActiveStatus(String id, Boolean isActive) {
        Log.debugf("Updating agent %s active status to: %s", id, isActive);
        try {
            UUID uuid = UUID.fromString(id);
            return update("isActive = ?1 WHERE id = ?2", isActive, uuid);
        } catch (IllegalArgumentException e) {
            Log.warnf("Invalid UUID format: %s", id);
            return Uni.createFrom().item(0L);
        }
    }

    public Uni<List<AgentEntity>> findWithFilters(
            String tenantId, 
            tech.kayys.wayang.agent.dto.AgentType agentType, 
            tech.kayys.wayang.agent.dto.AgentStatus status, 
            List<String> tags, 
            int page, 
            int size) {
        
        StringBuilder query = new StringBuilder("1=1");
        java.util.List<Object> params = new java.util.ArrayList<>();
        
        if (tenantId != null && !tenantId.isEmpty()) {
            query.append(" AND tenantId = ?");
            params.add(tenantId);
        }
        
        if (agentType != null) {
            query.append(" AND type = ?");
            params.add(agentType);
        }
        
        // Note: Status filtering would need to be mapped properly in a real implementation
        // AgentEntity uses isActive boolean, not AgentStatus enum directly
        
        String queryString = query.toString();
        Log.debugf("Finding agents with query: %s, params: %s", queryString, params);
        
        return find(queryString, params.toArray()).list(); // Simplified implementation
    }

    public Uni<AgentEntity> findByIdWithDetails(String agentId) {
        Log.debugf("Finding agent with details for ID: %s", agentId);
        return findById(agentId);
    }
}