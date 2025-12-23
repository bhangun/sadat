package tech.kayys.wayang.workflow.service;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import tech.kayys.wayang.workflow.domain.WorkflowSnapshotEntity;
import tech.kayys.wayang.workflow.model.WorkflowSnapshot;

import org.hibernate.annotations.Type;

import java.time.Instant;

/**
 * Repository for workflow snapshots
 */
@ApplicationScoped
public class WorkflowSnapshotStore implements PanacheRepositoryBase<WorkflowSnapshotEntity, String> {

    /**
     * Save snapshot
     */
    public Uni<Void> save(WorkflowSnapshot snapshot) {
        WorkflowSnapshotEntity entity = new WorkflowSnapshotEntity();
        entity.setId(java.util.UUID.randomUUID().toString());
        entity.setRunId(snapshot.runId());
        entity.setEventCount(snapshot.eventCount());
        entity.setSnapshotData(snapshot.state());
        entity.setCreatedAt(snapshot.createdAt());

        return persist(entity).replaceWithVoid();
    }

    /**
     * Get latest snapshot for run
     */
    public Uni<WorkflowSnapshot> getLatest(String runId) {
        return find("runId = ?1 ORDER BY eventCount DESC", runId)
                .firstResult()
                .map(entity -> entity != null
                        ? new WorkflowSnapshot(
                                entity.getRunId(),
                                entity.getSnapshotData(),
                                entity.getEventCount(),
                                entity.getCreatedAt())
                        : null);
    }

    /**
     * Delete old snapshots (keep only latest N)
     */
    public Uni<Long> deleteOldSnapshots(String runId, int keepCount) {
        return find("runId = ?1 ORDER BY eventCount DESC", runId)
                .page(keepCount, Integer.MAX_VALUE)
                .list()
                .onItem().transformToUni(
                        entities -> delete("id IN ?1", entities.stream().map(WorkflowSnapshotEntity::getId).toList()));
    }
}