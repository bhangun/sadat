package tech.kayys.wayang.workflow.repository;

import java.time.Instant;
import java.util.List;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.domain.Checkpoint;

/**
 * Checkpoint repository
 */
@ApplicationScoped
public class CheckpointRepository implements PanacheRepositoryBase<Checkpoint, String> {

    public Uni<Checkpoint> findLatestByRunId(String runId) {
        return find("runId = ?1 order by sequenceNumber desc", runId)
                .firstResult();
    }

    public Uni<List<Checkpoint>> findByRunId(String runId) {
        return find("runId = ?1 order by sequenceNumber desc", runId)
                .list();
    }

    public Uni<java.util.Optional<Integer>> getMaxSequence(String runId) {
        return find("select max(c.sequenceNumber) from Checkpoint c where c.runId = ?1", runId)
                .project(Integer.class)
                .firstResult()
                .map(result -> java.util.Optional.ofNullable(result));
    }

    public Uni<Void> deleteByRunId(String runId) {
        return delete("runId", runId).replaceWithVoid();
    }

    public Uni<Void> deleteByIds(List<String> ids) {
        if (ids.isEmpty()) {
            return Uni.createFrom().voidItem();
        }
        return delete("checkpointId in ?1", ids).replaceWithVoid();
    }

    public Uni<Long> deleteOldCheckpoints(Instant olderThan) {
        return delete("createdAt < ?1", olderThan);
    }
}