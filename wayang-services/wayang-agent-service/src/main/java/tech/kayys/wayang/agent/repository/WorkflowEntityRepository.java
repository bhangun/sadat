package tech.kayys.wayang.agent.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepository;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.entity.WorkflowEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class WorkflowEntityRepository implements PanacheRepository<WorkflowEntity> {

    public Uni<List<WorkflowEntity>> findByTenantId(String tenantId) {
        return find("tenantId", tenantId).list();
    }

    public Uni<Optional<WorkflowEntity>> findById(UUID id) {
        return findByIdOptional(id);
    }

    public Uni<List<WorkflowEntity>> findByTenantIdAndIsActive(String tenantId, Boolean isActive) {
        return find("tenantId = ?1 AND isActive = ?2", tenantId, isActive).list();
    }

    public Uni<Long> updateActiveStatus(UUID id, Boolean isActive) {
        return update("isActive = ?1 WHERE id = ?2", isActive, id);
    }
}