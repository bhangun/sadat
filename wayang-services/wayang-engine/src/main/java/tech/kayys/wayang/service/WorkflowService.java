package tech.kayys.wayang.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import tech.kayys.wayang.common.AuditEvent;
import tech.kayys.wayang.common.AuditService;
import tech.kayys.wayang.domain.Workflow;
import tech.kayys.wayang.exception.WorkflowImmutableException;
import tech.kayys.wayang.exception.WorkflowNotFoundException;
import tech.kayys.wayang.exception.WorkflowValidationException;
import tech.kayys.wayang.exception.WorkspaceNotFoundException;
import tech.kayys.wayang.model.LogicDefinition;
import tech.kayys.wayang.model.RuntimeConfig;
import tech.kayys.wayang.model.UIDefinition;
import tech.kayys.wayang.model.ValidationResult;
import tech.kayys.wayang.repository.WorkflowRepository;
import tech.kayys.wayang.repository.WorkspaceRepository;
import tech.kayys.wayang.tenant.TenantContext;

/**
 * WorkflowService - Core workflow management
 */
@ApplicationScoped
public class WorkflowService {

    private static final Logger LOG = Logger.getLogger(WorkflowService.class);

    @Inject
    WorkflowRepository workflowRepository;

    @Inject
    WorkspaceRepository workspaceRepository;

    @Inject
    ValidationService validationService;

    @Inject
    VersionService versionService;

    @Inject
    AuditService auditService;

    @Inject
    TenantContext tenantContext;

    /**
     * Create new workflow
     */
    @Transactional
    public Uni<Workflow> createWorkflow(UUID workspaceId, CreateWorkflowRequest request) {
        String tenantId = tenantContext.getTenantId();
        String userId = tenantContext.getUserId();

        LOG.infof("Creating workflow: %s in workspace: %s", request.name, workspaceId);

        return workspaceRepository.findByIdAndTenant(workspaceId, tenantId)
                .onItem().ifNull().failWith(() -> new WorkspaceNotFoundException(workspaceId, tenantId))
                .flatMap(workspace -> {
                    Workflow workflow = new Workflow();
                    workflow.workspace = workspace;
                    workflow.name = request.name;
                    workflow.description = request.description;
                    workflow.tenantId = tenantId;
                    workflow.version = request.version != null ? request.version : "1.0.0";
                    workflow.createdBy = userId;
                    workflow.status = Workflow.WorkflowStatus.DRAFT;

                    // Initialize empty structures
                    workflow.logic = request.logic != null ? request.logic : new LogicDefinition();
                    workflow.ui = request.ui != null ? request.ui : new UIDefinition();
                    workflow.runtime = request.runtime != null ? request.runtime : new RuntimeConfig();
                    workflow.metadata = request.metadata != null ? request.metadata : new HashMap<>();

                    return workflowRepository.persist(workflow);
                })
                .invoke(wf -> auditService.log(AuditEvent.builder()
                        .event("WORKFLOW_CREATED")
                        .actor(userId)
                        .tenantId(tenantId)
                        .targetId(wf.id.toString())
                        .metadata(Map.of(
                                "name", wf.name,
                                "version", wf.version,
                                "workspaceId", workspaceId.toString()))
                        .build()));
    }

    /**
     * Get workflow by ID with tenant validation
     */
    public Uni<Workflow> getWorkflow(UUID workflowId) {
        String tenantId = tenantContext.getTenantId();

        return workflowRepository.findByIdAndTenant(workflowId, tenantId)
                .onItem().ifNull().failWith(() -> new WorkflowNotFoundException(workflowId, tenantId));
    }

    /**
     * List workflows in workspace
     */
    public Uni<List<Workflow>> listWorkflows(UUID workspaceId) {
        String tenantId = tenantContext.getTenantId();

        return workspaceRepository.findByIdAndTenant(workspaceId, tenantId)
                .onItem().ifNull().failWith(() -> new WorkspaceNotFoundException(workspaceId, tenantId))
                .flatMap(ws -> workflowRepository.findByWorkspace(workspaceId, tenantId));
    }

    /**
     * Update workflow logic with optimistic locking
     */
    @Transactional
    public Uni<Workflow> updateLogic(UUID workflowId, LogicDefinition logic, Long expectedVersion) {
        String tenantId = tenantContext.getTenantId();
        String userId = tenantContext.getUserId();

        return getWorkflow(workflowId)
                .flatMap(workflow -> {
                    if (workflow.isPublished()) {
                        return Uni.createFrom().failure(
                                new WorkflowImmutableException(workflowId, "Workflow is published"));
                    }

                    // Validate logic before saving
                    return validationService.validateLogic(logic)
                            .flatMap(validationResult -> {
                                workflow.logic = logic;
                                workflow.validationResult = validationResult;
                                workflow.status = validationResult.isValid() ? Workflow.WorkflowStatus.VALID
                                        : Workflow.WorkflowStatus.INVALID;

                                return workflowRepository.persist(workflow)
                                        .invoke(wf -> auditService.log(AuditEvent.builder()
                                                .event("WORKFLOW_LOGIC_UPDATED")
                                                .actor(userId)
                                                .tenantId(tenantId)
                                                .targetId(wf.id.toString())
                                                .metadata(Map.of(
                                                        "nodeCount", logic.nodes.size(),
                                                        "connectionCount", logic.connections.size(),
                                                        "valid", validationResult.isValid()))
                                                .build()));
                            });
                });
    }

    /**
     * Update UI definition
     */
    @Transactional
    public Uni<Workflow> updateUI(UUID workflowId, UIDefinition ui) {
        String tenantId = tenantContext.getTenantId();
        String userId = tenantContext.getUserId();

        return getWorkflow(workflowId)
                .flatMap(workflow -> {
                    workflow.ui = ui;

                    return workflowRepository.persist(workflow)
                            .invoke(wf -> auditService.log(AuditEvent.builder()
                                    .event("WORKFLOW_UI_UPDATED")
                                    .actor(userId)
                                    .tenantId(tenantId)
                                    .targetId(wf.id.toString())
                                    .build()));
                });
    }

    /**
     * Validate workflow
     */
    @Retry(maxRetries = 2, delay = 500)
    @Timeout(value = 30000)
    public Uni<ValidationResult> validateWorkflow(UUID workflowId) {
        String tenantId = tenantContext.getTenantId();

        return getWorkflow(workflowId)
                .flatMap(workflow -> {
                    workflow.status = Workflow.WorkflowStatus.VALIDATING;

                    return workflowRepository.persist(workflow)
                            .flatMap(wf -> validationService.validateWorkflow(wf))
                            .flatMap(result -> workflowRepository.updateValidationResult(
                                    workflowId, tenantId, result)
                                    .replaceWith(result));
                });
    }

    /**
     * Publish workflow (create immutable version)
     */
    @Transactional
    public Uni<Workflow> publishWorkflow(UUID workflowId) {
        String tenantId = tenantContext.getTenantId();
        String userId = tenantContext.getUserId();

        LOG.infof("Publishing workflow: %s", workflowId);

        return getWorkflow(workflowId)
                .flatMap(workflow -> {
                    if (!workflow.canPublish()) {
                        return Uni.createFrom().failure(
                                new WorkflowValidationException(workflowId,
                                        "Workflow must be valid before publishing"));
                    }

                    workflow.status = Workflow.WorkflowStatus.PUBLISHED;
                    workflow.publishedAt = Instant.now();

                    return workflowRepository.persist(workflow)
                            .flatMap(published -> versionService.createVersion(published)
                                    .replaceWith(published))
                            .invoke(wf -> auditService.log(AuditEvent.builder()
                                    .event("WORKFLOW_PUBLISHED")
                                    .actor(userId)
                                    .tenantId(tenantId)
                                    .targetId(wf.id.toString())
                                    .metadata(Map.of(
                                            "name", wf.name,
                                            "version", wf.version))
                                    .build()));
                });
    }

    /**
     * Delete workflow (soft delete)
     */
    @Transactional
    public Uni<Void> deleteWorkflow(UUID workflowId) {
        String tenantId = tenantContext.getTenantId();
        String userId = tenantContext.getUserId();

        return workflowRepository.softDelete(workflowId, tenantId)
                .flatMap(deleted -> {
                    if (!deleted) {
                        return Uni.createFrom().failure(
                                new WorkflowNotFoundException(workflowId, tenantId));
                    }

                    return auditService.log(AuditEvent.builder()
                            .event("WORKFLOW_DELETED")
                            .actor(userId)
                            .tenantId(tenantId)
                            .targetId(workflowId.toString())
                            .build());
                })
                .replaceWithVoid();
    }

    // Request DTOs
    public static class CreateWorkflowRequest {
        public String name;
        public String description;
        public String version;
        public LogicDefinition logic;
        public UIDefinition ui;
        public RuntimeConfig runtime;
        public Map<String, Object> metadata;
    }
}
