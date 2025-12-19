package tech.kayys.wayang.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.Map;
import tech.kayys.wayang.dto.AuditEvent;
import tech.kayys.wayang.exception.DomainException;
import tech.kayys.wayang.exception.InternalException;
import jakarta.validation.ValidationException;

/**
 * ErrorHandlerService - Centralized error handling with audit
 */
@ApplicationScoped
public class ErrorHandlerService {

        private static final Logger LOG = Logger.getLogger(ErrorHandlerService.class);

        @Inject
        AuditService auditService;

        /**
         * Handle create error
         */
        public <T, I> Uni<T> handleCreateError(Throwable error, I input,
                        String tenantId, String userId) {

                LOG.errorf(error, "Create operation failed");

                // Audit error
                auditService.log(AuditEvent.builder()
                                .type("CREATE_FAILED")
                                .entityType(input.getClass().getSimpleName())
                                .userId(userId)
                                .tenantId(tenantId)
                                .metadata(Map.of(
                                                "error", error.getClass().getSimpleName(),
                                                "message", error.getMessage()))
                                .build());

                // Transform to appropriate exception
                return Uni.createFrom().failure(transformException(error));
        }

        /**
         * Handle update error
         */
        public <T, I> Uni<T> handleUpdateError(Throwable error, String id, I input,
                        String tenantId, String userId) {

                LOG.errorf(error, "Update operation failed: id=%s", id);

                // Audit error
                auditService.log(AuditEvent.builder()
                                .type("UPDATE_FAILED")
                                .entityId(id)
                                .userId(userId)
                                .tenantId(tenantId)
                                .metadata(Map.of(
                                                "error", error.getClass().getSimpleName(),
                                                "message", error.getMessage()))
                                .build());

                return Uni.createFrom().failure(transformException(error));
        }

        /**
         * Handle delete error
         */
        public void handleDeleteError(Throwable error, String id, String tenantId) {
                LOG.errorf(error, "Delete operation failed: id=%s", id);

                // Audit error
                auditService.log(AuditEvent.builder()
                                .type("DELETE_FAILED")
                                .entityId(id)
                                .tenantId(tenantId)
                                .metadata(Map.of(
                                                "error", error.getClass().getSimpleName(),
                                                "message", error.getMessage()))
                                .build());
        }

        /**
         * Handle query error
         */
        public <T> Uni<T> handleQueryError(Throwable error, String id, String tenantId) {
                LOG.errorf(error, "Query operation failed: id=%s", id);

                // Audit error
                auditService.log(AuditEvent.builder()
                                .type("QUERY_FAILED")
                                .entityId(id)
                                .tenantId(tenantId)
                                .metadata(Map.of(
                                                "error", error.getClass().getSimpleName(),
                                                "message", error.getMessage()))
                                .build());

                return Uni.createFrom().failure(transformException(error));
        }

        /**
         * Handle list error
         */
        public <T, F> Uni<T> handleListError(Throwable error, String tenantId, F filter) {
                LOG.errorf(error, "List operation failed");
                return Uni.createFrom().failure(transformException(error));
        }

        /**
         * Handle compare error
         */
        public <T> Uni<T> handleCompareError(Throwable error, String baseId,
                        String targetId, String tenantId) {
                LOG.errorf(error, "Compare operation failed: base=%s, target=%s",
                                baseId, targetId);
                return Uni.createFrom().failure(transformException(error));
        }

        /**
         * Handle node operations error
         */
        public <T, I> Uni<T> handleNodeAddError(Throwable error, String workflowId,
                        I input, String tenantId, String userId) {
                LOG.errorf(error, "Node add failed: workflow=%s", workflowId);

                auditService.log(AuditEvent.builder()
                                .type("NODE_ADD_FAILED")
                                .entityId(workflowId)
                                .userId(userId)
                                .tenantId(tenantId)
                                .metadata(Map.of("error", error.getMessage()))
                                .build());

                return Uni.createFrom().failure(transformException(error));
        }

        /**
         * Handle lock error
         */
        public <T> Uni<T> handleLockError(Throwable error, String workflowId,
                        String nodeId, String userId, String tenantId) {
                LOG.errorf(error, "Lock failed: workflow=%s, node=%s", workflowId, nodeId);

                auditService.log(AuditEvent.builder()
                                .type("LOCK_FAILED")
                                .entityId(workflowId)
                                .userId(userId)
                                .tenantId(tenantId)
                                .metadata(Map.of("nodeId", nodeId, "error", error.getMessage()))
                                .build());

                return Uni.createFrom().failure(transformException(error));
        }

        /**
         * Handle unlock error
         */
        public void handleUnlockError(Throwable error, String workflowId,
                        String nodeId, String userId, String tenantId) {
                LOG.errorf(error, "Unlock failed: workflow=%s, node=%s", workflowId, nodeId);

                auditService.log(AuditEvent.builder()
                                .type("UNLOCK_FAILED")
                                .entityId(workflowId)
                                .userId(userId)
                                .tenantId(tenantId)
                                .metadata(Map.of("nodeId", nodeId, "error", error.getMessage()))
                                .build());
        }

        /**
         * Transform exception to appropriate type
         */
        private Throwable transformException(Throwable error) {
                // Already a domain exception
                if (error instanceof DomainException) {
                        return error;
                }

                // Database errors
                if (error.getMessage().contains("constraint")) {
                        return new ValidationException("Constraint violation: " + error.getMessage());
                }

                // Wrap as internal error
                return new InternalException("Internal error: " + error.getMessage(), error);
        }
}