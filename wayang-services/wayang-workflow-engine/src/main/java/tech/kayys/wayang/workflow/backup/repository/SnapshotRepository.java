package tech.kayys.wayang.workflow.backup.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.backup.service.SnapshotCache;
import tech.kayys.wayang.workflow.model.WorkflowSnapshot;

/**
 * Repository for managing workflow snapshots
 */
@ApplicationScoped
public class SnapshotRepository implements PanacheRepositoryBase<WorkflowSnapshot, String> {

    private static final Logger log = LoggerFactory.getLogger(SnapshotRepository.class);

    @Inject
    SnapshotCache snapshotCache;

    /**
     * Find all snapshots
     */
    public Uni<List<WorkflowSnapshot>> getAllSnapshots() {
        return listAll();
    }

    /**
     * Find snapshots modified since a specific time
     */
    public Uni<List<WorkflowSnapshot>> findModifiedSince(Instant since) {
        log.info("Find snapshots modified since " + since);
        return find("lastModified >= ?1", since).list();
    }

    /**
     * Find snapshot by ID
     */
    public Uni<Optional<WorkflowSnapshot>> getSnapshotById(String snapshotId) {
        WorkflowSnapshot cached = snapshotCache.get(snapshotId);
        if (cached != null) {
            return Uni.createFrom().item(Optional.of(cached));
        }

        return findById(snapshotId)
                .map(Optional::ofNullable);
    }

    /**
     * Save or update snapshot
     */
    public Uni<WorkflowSnapshot> saveSnapshot(WorkflowSnapshot snapshot) {
        if (snapshot.getId() == null) {
            snapshot.setId(UUID.randomUUID().toString());
            snapshot.setCreatedAt(Instant.now());
            snapshot.setLastModified(snapshot.getCreatedAt());
            return persist(snapshot)
                    .onItem().invoke(saved -> snapshotCache.put(saved.getId(), saved));
        } else {
            snapshot.setLastModified(Instant.now());
            return getSession().onItem().transformToUni(session -> session.merge(snapshot))
                    .onItem().invoke(saved -> snapshotCache.put(saved.getId(), saved));
        }
    }

    /**
     * Delete snapshot
     */
    public Uni<Boolean> deleteSnapshot(String snapshotId) {
        return deleteById(snapshotId)
                .onItem().invoke(deleted -> {
                    if (deleted) {
                        snapshotCache.remove(snapshotId);
                    }
                });
    }
}
