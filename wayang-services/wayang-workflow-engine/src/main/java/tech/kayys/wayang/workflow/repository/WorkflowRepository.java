package tech.kayys.wayang.workflow.repository;

import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.workflow.domain.WorkflowEntity;
import tech.kayys.wayang.workflow.domain.WorkflowRun;

/**
 * Workflow repository for persistence.
 */
@ApplicationScoped
public class WorkflowRepository implements PanacheRepositoryBase<WorkflowEntity, Long> {

    public Uni<WorkflowDefinition> save(WorkflowDefinition workflow, String tenantId) {
        WorkflowEntity entity = WorkflowEntity.fromDefinition(workflow, tenantId);
        return persist(entity)
                .map(WorkflowEntity::toDefinition);
    }

    public Uni<WorkflowDefinition> findById(String workflowId, String tenantId) {
        return find("id = ?1 and tenantId = ?2", workflowId, tenantId)
                .firstResult()
                .map(entity -> entity != null ? entity.toDefinition() : null);
    }

    public Uni<List<WorkflowDefinition>> findByTenant(String tenantId, int page, int size) {
        return find("tenantId = ?1 order by createdAt desc", tenantId)
                .page(page, size)
                .list()
                .map(entities -> entities.stream()
                        .map(WorkflowEntity::toDefinition)
                        .toList());
    }

    public Uni<WorkflowDefinition> update(WorkflowDefinition workflow, String tenantId) {
        return findById(workflow.getId().getValue(), tenantId)
                .onItem().transformToUni(entity -> {
                    if (entity == null) {
                        return Uni.createFrom().nullItem();
                    }
                    WorkflowEntity updated = WorkflowEntity.fromDefinition(workflow, tenantId);
                    return persist(updated).map(WorkflowEntity::toDefinition);
                });
    }

    public Uni<Void> delete(String workflowId, String tenantId) {
        return delete("id = ?1 and tenantId = ?2", workflowId, tenantId)
                .replaceWithVoid();
    }

    public Uni<List<WorkflowRun>> findRuns(String workflowId, String tenantId, int limit) {
        // Query from StateStore
        return Uni.createFrom().item(List.of());
    }
}