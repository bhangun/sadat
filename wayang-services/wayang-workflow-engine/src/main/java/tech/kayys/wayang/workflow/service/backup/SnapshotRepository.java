package tech.kayys.wayang.workflow.service.backup;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import tech.kayys.wayang.workflow.model.WorkflowSnapshot;

/**
 * Repository for managing workflow snapshots
 */
@ApplicationScoped
public class SnapshotRepository {

    private static final Logger log = LoggerFactory.getLogger(SnapshotRepository.class);

    @Inject
    EntityManager entityManager;

    @Inject
    SnapshotCache snapshotCache;

    /**
     * Find all snapshots
     */
    public Uni<List<WorkflowSnapshot>> findAll() {
        return Uni.createFrom().item(() -> entityManager.createQuery("FROM WorkflowSnapshot", WorkflowSnapshot.class)
                .getResultList());
    }

    /**
     * Find snapshots modified since a specific time
     */
    public Uni<List<WorkflowSnapshot>> findModifiedSince(Instant since) {
        return Uni.createFrom().item(() -> entityManager.createQuery(
                "FROM WorkflowSnapshot WHERE lastModified >= :since",
                WorkflowSnapshot.class)
                .setParameter("since", since)
                .getResultList());
    }

    /**
     * Find snapshot by ID
     */
    public Uni<Optional<WorkflowSnapshot>> findById(String snapshotId) {
        return Uni.createFrom().item(() -> {
            WorkflowSnapshot snapshot = snapshotCache.get(snapshotId);
            if (snapshot != null) {
                return Optional.of(snapshot);
            }

            snapshot = entityManager.find(WorkflowSnapshot.class, snapshotId);
            return Optional.ofNullable(snapshot);
        });
    }

    /**
     * Save or update snapshot
     */
    public Uni<WorkflowSnapshot> save(WorkflowSnapshot snapshot) {
        return Uni.createFrom().item(() -> {
            if (snapshot.getId() == null) {
                snapshot.setId(UUID.randomUUID().toString());
                snapshot.setCreatedAt(Instant.now());
                entityManager.persist(snapshot);
            } else {
                snapshot.setLastModified(Instant.now());
                snapshot = entityManager.merge(snapshot);
            }

            snapshotCache.put(snapshot.getId(), snapshot);
            log.debug("Saved snapshot: {}", snapshot.getId());
            return snapshot;
        });
    }

    /**
     * Delete snapshot
     */
    public Uni<Boolean> delete(String snapshotId) {
        return findById(snapshotId)
                .onItem().transform(optionalSnapshot -> {
                    if (optionalSnapshot.isPresent()) {
                        entityManager.remove(optionalSnapshot.get());
                        snapshotCache.remove(snapshotId);
                        log.debug("Deleted snapshot: {}", snapshotId);
                        return true;
                    }
                    return false;
                });
    }
}
