package tech.kayys.wayang.workflow.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.domain.SagaExecutionEntity;
import tech.kayys.wayang.workflow.model.saga.SagaExecution;

/**
 * Repository for Saga executions
 */
@ApplicationScoped
public class SagaExecutionRepository implements PanacheRepositoryBase<SagaExecutionEntity, String> {

    public Uni<SagaExecution> save(SagaExecution execution) {
        SagaExecutionEntity entity = toEntity(execution);
        return persist(entity).map(this::toDomain);
    }

    public Uni<SagaExecution> update(SagaExecution execution) {
        return findById(execution.getId())
                .onItem().transformToUni(entity -> {
                    updateEntity(entity, execution);
                    return persist(entity).map(this::toDomain);
                });
    }

    private SagaExecutionEntity toEntity(SagaExecution execution) {
        // Convert to entity
        return new SagaExecutionEntity();
    }

    private void updateEntity(SagaExecutionEntity entity, SagaExecution execution) {
        entity.setStatus(execution.getStatus());
        entity.setCompletedAt(execution.getCompletedAt());
        entity.setErrorMessage(execution.getErrorMessage());
    }

    private SagaExecution toDomain(SagaExecutionEntity entity) {
        // Convert to domain
        return new SagaExecution(
                entity.getId(),
                entity.getRunId(),
                entity.getSagaDefId(),
                entity.getStrategy(),
                entity.getStatus(),
                entity.getStartedAt());
    }
}
