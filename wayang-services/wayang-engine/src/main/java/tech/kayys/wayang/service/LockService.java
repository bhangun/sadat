package tech.kayys.wayang.service;

import java.util.Optional;
import java.util.UUID;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import tech.kayys.wayang.domain.WorkflowLock;
import tech.kayys.wayang.exception.WorkflowLockedException;
import tech.kayys.wayang.repository.WorkflowLockRepository;
import tech.kayys.wayang.tenant.TenantContext;

/**
 * LockService - Concurrent editing lock management
 */
@ApplicationScoped
public class LockService {

    private static final Logger LOG = Logger.getLogger(LockService.class);

    @Inject
    WorkflowLockRepository lockRepository;

    @Inject
    TenantContext tenantContext;

    /**
     * Acquire lock on workflow
     */
    @Transactional
    public Uni<WorkflowLock> acquireLock(UUID workflowId, String sessionId) {
        String userId = tenantContext.getUserId();

        LOG.infof("User %s attempting to acquire lock on workflow %s", userId, workflowId);

        return lockRepository.acquireLock(workflowId, userId, sessionId)
                .onFailure(WorkflowLockRepository.LockAcquisitionException.class)
                .transform(e -> new WorkflowLockedException(workflowId, e.getMessage()));
    }

    /**
     * Renew lock heartbeat
     */
    @Transactional
    public Uni<WorkflowLock> renewLock(UUID lockId) {
        return lockRepository.renewLock(lockId);
    }

    /**
     * Release lock
     */
    @Transactional
    public Uni<Void> releaseLock(UUID lockId) {
        String userId = tenantContext.getUserId();

        return lockRepository.releaseLock(lockId, userId)
                .replaceWithVoid();
    }

    /**
     * Check if workflow is locked
     */
    public Uni<Optional<WorkflowLock>> checkLock(UUID workflowId) {
        return lockRepository.findActiveLock(workflowId)
                .map(Optional::ofNullable);
    }
}
