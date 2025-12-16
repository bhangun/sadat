package tech.kayys.wayang.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import tech.kayys.wayang.common.AuditEvent;
import tech.kayys.wayang.common.AuditService;
import tech.kayys.wayang.domain.Workspace;
import tech.kayys.wayang.exception.WorkspaceCreationException;
import tech.kayys.wayang.exception.WorkspaceNotFoundException;
import tech.kayys.wayang.repository.WorkflowRepository;
import tech.kayys.wayang.repository.WorkspaceRepository;
import tech.kayys.wayang.tenant.TenantContext;

import org.jboss.logging.Logger;

import java.util.*;

/**
 * WorkspaceService - Core workspace management
 */
@ApplicationScoped
public class WorkspaceService {

    private static final Logger LOG = Logger.getLogger(WorkspaceService.class);

    @Inject
    WorkspaceRepository workspaceRepository;

    @Inject
    WorkflowRepository workflowRepository;

    @Inject
    AuditService auditService;

    @Inject
    TenantContext tenantContext;

    /**
     * Create new workspace with multi-tenant validation
     */
    @Transactional
    public Uni<Workspace> createWorkspace(CreateWorkspaceRequest request) {
        String tenantId = tenantContext.getTenantId();
        String userId = tenantContext.getUserId();

        LOG.infof("Creating workspace: %s for tenant: %s", request.name, tenantId);

        Workspace workspace = new Workspace();
        workspace.name = request.name;
        workspace.description = request.description;
        workspace.tenantId = tenantId;
        workspace.ownerId = userId;
        workspace.metadata = request.metadata != null ? request.metadata : new HashMap<>();
        workspace.status = Workspace.WorkspaceStatus.ACTIVE;

        return workspaceRepository.persist(workspace)
                .invoke(ws -> auditService.log(AuditEvent.builder()
                        .event("WORKSPACE_CREATED")
                        .actor(userId)
                        .tenantId(tenantId)
                        .targetId(ws.id.toString())
                        .metadata(Map.of("name", ws.name))
                        .build()))
                .onFailure().transform(t -> new WorkspaceCreationException(
                        "Failed to create workspace: " + request.name, t));
    }

    /**
     * Get workspace by ID with tenant validation
     */
    public Uni<Workspace> getWorkspace(UUID workspaceId) {
        String tenantId = tenantContext.getTenantId();

        return workspaceRepository.findByIdAndTenant(workspaceId, tenantId)
                .onItem().ifNull().failWith(() -> new WorkspaceNotFoundException(workspaceId, tenantId));
    }

    /**
     * List all workspaces for current tenant
     */
    public Uni<List<Workspace>> listWorkspaces() {
        String tenantId = tenantContext.getTenantId();
        return workspaceRepository.findByTenant(tenantId);
    }

    /**
     * Update workspace
     */
    @Transactional
    public Uni<Workspace> updateWorkspace(UUID workspaceId, UpdateWorkspaceRequest request) {
        String tenantId = tenantContext.getTenantId();
        String userId = tenantContext.getUserId();

        return getWorkspace(workspaceId)
                .flatMap(workspace -> {
                    workspace.name = request.name != null ? request.name : workspace.name;
                    workspace.description = request.description != null ? request.description : workspace.description;
                    workspace.metadata.putAll(request.metadata != null ? request.metadata : Collections.emptyMap());

                    return workspaceRepository.persist(workspace)
                            .invoke(ws -> auditService.log(AuditEvent.builder()
                                    .event("WORKSPACE_UPDATED")
                                    .actor(userId)
                                    .tenantId(tenantId)
                                    .targetId(ws.id.toString())
                                    .build()));
                });
    }

    /**
     * Delete workspace (soft delete)
     */
    @Transactional
    public Uni<Void> deleteWorkspace(UUID workspaceId) {
        String tenantId = tenantContext.getTenantId();
        String userId = tenantContext.getUserId();

        return workspaceRepository.softDelete(workspaceId, tenantId)
                .flatMap(deleted -> {
                    if (!deleted) {
                        return Uni.createFrom().failure(
                                new WorkspaceNotFoundException(workspaceId, tenantId));
                    }

                    return auditService.log(AuditEvent.builder()
                            .event("WORKSPACE_DELETED")
                            .actor(userId)
                            .tenantId(tenantId)
                            .targetId(workspaceId.toString())
                            .build());
                })
                .replaceWithVoid();
    }

    // Request DTOs
    public static class CreateWorkspaceRequest {
        public String name;
        public String description;
        public Map<String, Object> metadata;
    }

    public static class UpdateWorkspaceRequest {
        public String name;
        public String description;
        public Map<String, Object> metadata;
    }
}
