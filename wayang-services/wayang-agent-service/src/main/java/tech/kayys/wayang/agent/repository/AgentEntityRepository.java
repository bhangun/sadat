package tech.kayys.wayang.agent.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.entity.AgentEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AgentEntityRepository implements PanacheRepository<AgentEntity> {

    public Uni<List<AgentEntity>> findByTenantId(String tenantId) {
        return find("tenantId", tenantId).list();
    }

    public Uni<AgentEntity> findById(UUID id) {
        return find("id", id).firstResult();
    }

    public Uni<List<AgentEntity>> findByTenantIdAndIsActive(String tenantId, Boolean isActive) {
        return find("tenantId = ?1 AND isActive = ?2", tenantId, isActive).list();
    }

    public Uni<Long> updateActiveStatus(UUID id, Boolean isActive) {
        return update("isActive = ?1 WHERE id = ?2", isActive, id);
    }
}