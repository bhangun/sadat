package tech.kayys.wayang.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import tech.kayys.wayang.repository.WorkflowRepository;

import org.jboss.logging.Logger;

import tech.kayys.wayang.domain.Workflow;
import tech.kayys.wayang.domain.Workflow.WorkflowStatus;
import tech.kayys.wayang.dto.AuditEvent;
import tech.kayys.wayang.schema.NodeLockDTO;
import tech.kayys.wayang.exception.NodeLockedException;
import tech.kayys.wayang.exception.NodeNotFoundException;
import tech.kayys.wayang.exception.WorkflowDeletionException;
import tech.kayys.wayang.exception.WorkflowLockedException;
import tech.kayys.wayang.exception.WorkflowNotFoundException;
import tech.kayys.wayang.mapper.WorkflowMapper;
import tech.kayys.wayang.model.LogicDefinition;
import tech.kayys.wayang.schema.CreateWorkflowInput;
import tech.kayys.wayang.schema.NodeDTO;
import tech.kayys.wayang.schema.NodeInput;
import tech.kayys.wayang.schema.UpdateWorkflowInput;
import tech.kayys.wayang.schema.WorkflowDTO;
import tech.kayys.wayang.schema.node.NodeDefinition;

import java.time.Instant;
import java.util.*;

/**
 * WorkflowCommandService - Handles write operations with audit
 */
@ApplicationScoped
public class WorkflowCommandService {

    private static final Logger LOG = Logger.getLogger(WorkflowCommandService.class);

    @Inject
    WorkflowRepository repository;

    @Inject
    WorkflowValidator validator;

    @Inject
    AuditService auditService;

    @Inject
    WorkflowMapper mapper;

    @Inject
    ErrorHandlerService errorHandler;

    /**
     * Create new workflow with validation and audit
     */
    @Transactional
    public Uni<WorkflowDTO> create(CreateWorkflowInput input, String tenantId, String userId) {
        LOG.infof("Creating workflow: name=%s, tenant=%s, user=%s",
                input.getName(), tenantId, userId);

        return Uni.createFrom().item(() -> {
            // Validate input
            validator.validateCreate(input);

            // Create entity
            // Create entity
            Workflow entity = new Workflow();
            entity.id = UUID.randomUUID();
            entity.version = "1.0.0";
            entity.name = input.getName();
            entity.description = input.getDescription();
            entity.status = WorkflowStatus.DRAFT;
            entity.tenantId = tenantId;
            entity.createdBy = userId;
            entity.createdAt = Instant.now();
            entity.updatedAt = Instant.now();

            // Set components
            entity.logic = mapper.toLogicEntity(input.getLogic());
            entity.ui = mapper.toUIEntity(input.getUi());
            entity.runtime = mapper.toRuntimeEntity(input.getRuntime());
            entity.tags = new ArrayList<>(input.getTags());
            entity.metadata = input.getMetadata();

            return entity;
        })
                .flatMap(entity -> repository.persist(entity))
                .invoke(entity -> {
                    // Audit trail
                    auditService.log(new AuditEvent.Builder()
                            .type("WORKFLOW_CREATED")
                            .entityType("Workflow")
                            .entityId(entity.id.toString())
                            .userId(userId)
                            .tenantId(tenantId)
                            .metadata(Map.of(
                                    "name", entity.name,
                                    "version", entity.version))
                            .build());
                })
                .map(mapper::toDTO)
                .onFailure().recoverWithUni(throwable -> {
                    LOG.errorf(throwable, "Failed to create workflow: %s", input.getName());
                    return errorHandler.handleCreateError(throwable, input, tenantId, userId);
                });
    }

    /**
     * Update workflow with optimistic locking and audit
     */
    @Transactional
    public Uni<WorkflowDTO> update(String id, UpdateWorkflowInput input,
            String tenantId, String userId) {

        LOG.infof("Updating workflow: id=%s, tenant=%s, user=%s", id, tenantId, userId);

        return repository.findByIdAndTenant(UUID.fromString(id), tenantId)
                .onItem().ifNull().failWith(() -> new WorkflowNotFoundException("Workflow not found: " + id))
                .invoke(entity -> {
                    // Validate update
                    validator.validateUpdate(entity, input);

                    // Check if locked by another user
                    if (entity.locked && !entity.lockedBy.equals(userId)) {
                        throw new WorkflowLockedException(
                                "Workflow locked by: " + entity.lockedBy);
                    }
                })
                .invoke(entity -> {
                    // Apply updates
                    if (input.getName() != null) {
                        entity.name = input.getName();
                    }
                    if (input.getDescription() != null) {
                        entity.description = input.getDescription();
                    }
                    if (input.getLogic() != null) {
                        entity.logic = mapper.toLogicEntity(input.getLogic());
                    }
                    if (input.getUi() != null) {
                        entity.ui = mapper.toUIEntity(input.getUi());
                    }
                    if (input.getRuntime() != null) {
                        entity.runtime = mapper.toRuntimeEntity(input.getRuntime());
                    }
                    if (input.getTags() != null) {
                        entity.tags = input.getTags();
                    }

                    entity.updatedAt = Instant.now();
                    entity.lastModifiedBy = userId;
                    entity.status = WorkflowStatus.DRAFT; // Reset to draft on update
                })
                .flatMap(entity -> repository.persist(entity))
                .invoke(entity -> {
                    // Audit trail
                    auditService.log(new AuditEvent.Builder()
                            .type("WORKFLOW_UPDATED")
                            .entityType("Workflow")
                            .entityId(entity.id.toString())
                            .userId(userId)
                            .tenantId(tenantId)
                            .changes(mapper.buildChangeSet(entity, input))
                            .build());
                })
                .map(mapper::toDTO)
                .onFailure().recoverWithUni(throwable -> {
                    LOG.errorf(throwable, "Failed to update workflow: %s", id);
                    return errorHandler.handleUpdateError(throwable, id, input, tenantId, userId);
                });
    }

    /**
     * Delete workflow with cascade and audit
     */
    @Transactional
    public Uni<Boolean> delete(String id, String tenantId) {
        LOG.infof("Deleting workflow: id=%s, tenant=%s", id, tenantId);

        return repository.findByIdAndTenant(UUID.fromString(id), tenantId)
                .onItem().ifNull().failWith(() -> new WorkflowNotFoundException("Workflow not found: " + id))
                .invoke(entity -> {
                    // Validate deletion
                    if (entity.status == WorkflowStatus.PUBLISHED) {
                        throw new WorkflowDeletionException(
                                "Cannot delete published workflow. Archive it first.");
                    }
                })
                .flatMap(entity -> repository.delete(entity)
                        .map(v -> true))
                .invoke(deleted -> {
                    // Audit trail
                    auditService.log(new AuditEvent.Builder()
                            .type("WORKFLOW_DELETED")
                            .entityType("Workflow")
                            .entityId(id)
                            .tenantId(tenantId)
                            .build());
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.errorf(throwable, "Failed to delete workflow: %s", id);
                    errorHandler.handleDeleteError(throwable, id, tenantId);
                    return false;
                });
    }

    /**
     * Add node to workflow with validation
     */
    @Transactional
    public Uni<NodeDTO> addNode(String workflowId, NodeInput input,
            String tenantId, String userId) {

        LOG.infof("Adding node to workflow: workflow=%s, type=%s",
                workflowId, input.getType());

        return repository.findByIdAndTenant(UUID.fromString(workflowId), tenantId)
                .onItem().ifNull().failWith(() -> new WorkflowNotFoundException("Workflow not found: " + workflowId))
                .invoke(entity -> {
                    // Validate node
                    validator.validateNode(entity, input);

                    // Create node
                    NodeDefinition node = new NodeDefinition();
                    node.id = UUID.randomUUID().toString();
                    node.type = input.getType();
                    node.name = input.getName();
                    node.properties = input.getProperties();
                    node.metadata = input.getMetadata();
                    node.createdAt = Instant.now();

                    // Add to logic
                    if (entity.logic == null) {
                        entity.logic = new LogicDefinition();
                    }
                    entity.logic.nodes.add(node);
                    entity.updatedAt = Instant.now();
                    entity.status = WorkflowStatus.DRAFT;
                })
                .flatMap(entity -> repository.persist(entity))
                .map(entity -> {
                    // Find the added node
                    NodeDefinition node = entity.logic.nodes.stream()
                            .filter(n -> n.type.equals(input.getType()) &&
                                    n.name.equals(input.getName()))
                            .findFirst()
                            .orElseThrow();
                    return mapper.toNodeDTO(node);
                })
                .invoke(node -> {
                    // Audit trail
                    auditService.log(new AuditEvent.Builder()
                            .type("NODE_ADDED")
                            .entityType("Workflow")
                            .entityId(workflowId)
                            .userId(userId)
                            .tenantId(tenantId)
                            .metadata(Map.of(
                                    "nodeId", node.getId(),
                                    "nodeType", node.getType(),
                                    "nodeName", node.getName()))
                            .build());
                })
                .onFailure().recoverWithUni(throwable -> {
                    LOG.errorf(throwable, "Failed to add node: %s", input.getName());
                    return errorHandler.handleNodeAddError(
                            throwable, workflowId, input, tenantId, userId);
                });
    }

    /**
     * Lock node for editing
     */
    @Transactional
    public Uni<NodeLockDTO> lockNode(String workflowId, String nodeId,
            String userId, String tenantId) {

        LOG.infof("Locking node: workflow=%s, node=%s, user=%s",
                workflowId, nodeId, userId);

        return repository.findByIdAndTenant(UUID.fromString(workflowId), tenantId)
                .onItem().ifNull().failWith(() -> new WorkflowNotFoundException("Workflow not found: " + workflowId))
                .invoke(entity -> {
                    // Find node
                    NodeDefinition node = findNode(entity, nodeId);

                    // Check if already locked
                    if (node.locked && !node.lockedBy.equals(userId)) {
                        throw new NodeLockedException(
                                "Node locked by: " + node.lockedBy);
                    }

                    // Lock node
                    node.locked = true;
                    node.lockedBy = userId;
                    node.lockedAt = Instant.now();
                })
                .flatMap(entity -> repository.persist(entity))
                .map(entity -> {
                    NodeDefinition node = findNode(entity, nodeId);
                    NodeLockDTO lock = new NodeLockDTO();
                    lock.setNodeId(nodeId);
                    lock.setUserId(userId);
                    lock.setLockedAt(node.lockedAt);
                    lock.setExpiresAt(node.lockedAt.plusSeconds(300)); // 5 min
                    return lock;
                })
                .invoke(lock -> {
                    // Audit trail
                    auditService.log(new AuditEvent.Builder()
                            .type("NODE_LOCKED")
                            .entityType("Workflow")
                            .entityId(workflowId)
                            .userId(userId)
                            .tenantId(tenantId)
                            .metadata(Map.of("nodeId", nodeId))
                            .build());
                })
                .onFailure().recoverWithUni(throwable -> {
                    LOG.errorf(throwable, "Failed to lock node: %s", nodeId);
                    return errorHandler.handleLockError(
                            throwable, workflowId, nodeId, userId, tenantId);
                });
    }

    /**
     * Unlock node
     */
    @Transactional
    public Uni<Boolean> unlockNode(String workflowId, String nodeId,
            String userId, String tenantId) {

        LOG.infof("Unlocking node: workflow=%s, node=%s, user=%s",
                workflowId, nodeId, userId);

        return repository.findByIdAndTenant(UUID.fromString(workflowId), tenantId)
                .onItem().ifNull().failWith(() -> new WorkflowNotFoundException("Workflow not found: " + workflowId))
                .invoke(entity -> {
                    // Find node
                    NodeDefinition node = findNode(entity, nodeId);

                    // Check ownership
                    if (node.locked && !node.lockedBy.equals(userId)) {
                        throw new NodeLockedException(
                                "Node locked by different user: " + node.lockedBy);
                    }

                    // Unlock node
                    node.locked = false;
                    node.lockedBy = null;
                    node.lockedAt = null;
                })
                .flatMap(entity -> repository.persist(entity))
                .map(entity -> true)
                .invoke(success -> {
                    // Audit trail
                    auditService.log(new AuditEvent.Builder()
                            .type("NODE_UNLOCKED")
                            .entityType("Workflow")
                            .entityId(workflowId)
                            .userId(userId)
                            .tenantId(tenantId)
                            .metadata(Map.of("nodeId", nodeId))
                            .build());
                })
                .onFailure().recoverWithItem(throwable -> {
                    LOG.errorf(throwable, "Failed to unlock node: %s", nodeId);
                    errorHandler.handleUnlockError(
                            throwable, workflowId, nodeId, userId, tenantId);
                    return false;
                });
    }

    /**
     * Helper: Find node in workflow
     */
    private NodeDefinition findNode(Workflow workflow, String nodeId) {
        return workflow.logic.nodes.stream()
                .filter(n -> n.id.equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new NodeNotFoundException("Node not found: " + nodeId));
    }
}