package tech.kayys.wayang.repository;

import java.time.Instant;
import java.util.UUID;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.domain.WorkflowLock;

/**
 * WorkflowLockRepository - Concurrent editing lock management
 */
@ApplicationScoped
public class WorkflowLockRepository implements PanacheRepositoryBase<WorkflowLock, UUID> {

    private static final long LOCK_TTL_SECONDS = 300; // 5 minutes

    /**
     * Acquire exclusive lock
     */
    public Uni<WorkflowLock> acquireLock(UUID workflowId, String userId, String sessionId) {
        return findActiveLock(workflowId)
                .onItem().transformToUni(existingLock -> {
                    if (existingLock != null && !existingLock.isExpired()) {
                        if (existingLock.userId.equals(userId) &&
                                existingLock.sessionId.equals(sessionId)) {
                            // Renew existing lock
                            return renewLock(existingLock.id);
                        }
                        // Lock held by another user
                        return Uni.createFrom().failure(
                                new LockAcquisitionException("Workflow locked by: " + existingLock.userId));
                    }

                    // Create new lock
                    WorkflowLock lock = new WorkflowLock();
                    lock.workflowId = workflowId;
                    lock.userId = userId;
                    lock.sessionId = sessionId;
                    lock.acquiredAt = Instant.now();
                    lock.expiresAt = Instant.now().plusSeconds(LOCK_TTL_SECONDS);
                    lock.heartbeatAt = lock.acquiredAt;
                    lock.type = WorkflowLock.LockType.EXCLUSIVE;

                    return persist(lock);
                });
    }

    /**
     * Find active lock for workflow
     */
    public Uni<WorkflowLock> findActiveLock(UUID workflowId) {
        return find("workflowId = :workflowId and expiresAt > :now order by acquiredAt desc",
                Parameters.with("workflowId", workflowId)
                        .and("now", Instant.now()))
                .firstResult();
    }

    /**
     * Renew lock heartbeat
     */
    public Uni<WorkflowLock> renewLock(UUID lockId) {
        Instant now = Instant.now();
        return update("expiresAt = :expiresAt, heartbeatAt = :heartbeat where id = :id",
                Parameters.with("expiresAt", now.plusSeconds(LOCK_TTL_SECONDS))
                        .and("heartbeat", now)
                        .and("id", lockId))
                .flatMap(count -> findById(lockId));
    }

    /**
     * Release lock
     */
    public Uni<Boolean> releaseLock(UUID lockId, String userId) {
        return delete("id = :id and userId = :userId",
                Parameters.with("id", lockId).and("userId", userId))
                .map(count -> count > 0);
    }

    /**
     * Clean expired locks (scheduled task)
     */
    public Uni<Long> cleanExpiredLocks() {
        return delete("expiresAt < :now",
                Parameters.with("now", Instant.now()));
    }

    public static class LockAcquisitionException extends RuntimeException {
        public LockAcquisitionException(String message) {
            super(message);
        }
    }
}
