package tech.kayys.wayang.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.domain.Workflow;
import tech.kayys.wayang.model.LogicDefinition;
import tech.kayys.wayang.model.UIDefinition;
import tech.kayys.wayang.model.ValidationResult;

/**
 * WorkflowRepository - Multi-tenant workflow data access
 */
@ApplicationScoped
public class WorkflowRepository implements PanacheRepositoryBase<Workflow, UUID> {

    /**
     * Find workflows in workspace with tenant validation
     */
    public Uni<List<Workflow>> findByWorkspace(UUID workspaceId, String tenantId) {
        return find("workspace.id = :workspaceId and tenantId = :tenantId and status != :deleted",
                Parameters.with("workspaceId", workspaceId)
                        .and("tenantId", tenantId)
                        .and("deleted", Workflow.WorkflowStatus.DELETED))
                .list();
    }

    /**
     * Find workflow by ID with tenant validation
     */
    public Uni<Workflow> findByIdAndTenant(UUID id, String tenantId) {
        return find("id = :id and tenantId = :tenantId and status != :deleted",
                Parameters.with("id", id)
                        .and("tenantId", tenantId)
                        .and("deleted", Workflow.WorkflowStatus.DELETED))
                .firstResult();
    }

    /**
     * Find latest version of workflow by name
     */
    public Uni<Workflow> findLatestVersion(UUID workspaceId, String name, String tenantId) {
        return find("workspace.id = :workspaceId and name = :name and tenantId = :tenantId " +
                "and status != :deleted order by createdAt desc",
                Parameters.with("workspaceId", workspaceId)
                        .and("name", name)
                        .and("tenantId", tenantId)
                        .and("deleted", Workflow.WorkflowStatus.DELETED))
                .firstResult();
    }

    /**
     * Find all published versions
     */
    public Uni<List<Workflow>> findPublishedVersions(UUID workspaceId, String name, String tenantId) {
        return find("workspace.id = :workspaceId and name = :name and tenantId = :tenantId " +
                "and status = :published order by publishedAt desc",
                Parameters.with("workspaceId", workspaceId)
                        .and("name", name)
                        .and("tenantId", tenantId)
                        .and("published", Workflow.WorkflowStatus.PUBLISHED))
                .list();
    }

    /**
     * Soft delete workflow
     */
    public Uni<Boolean> softDelete(UUID id, String tenantId) {
        return update("status = :status, updatedAt = :now where id = :id and tenantId = :tenantId",
                Parameters.with("status", Workflow.WorkflowStatus.DELETED)
                        .and("now", Instant.now())
                        .and("id", id)
                        .and("tenantId", tenantId))
                .map(count -> count > 0);
    }

    /**
     * Update workflow logic with optimistic locking
     */
    public Uni<Boolean> updateLogic(UUID id, String tenantId, LogicDefinition logic, Long expectedVersion) {
        return update("logic = :logic, updatedAt = :now where id = :id and tenantId = :tenantId " +
                "and entityVersion = :version",
                Parameters.with("logic", logic)
                        .and("now", Instant.now())
                        .and("id", id)
                        .and("tenantId", tenantId)
                        .and("version", expectedVersion))
                .map(count -> count > 0);
    }

    /**
     * Update UI definition
     */
    public Uni<Boolean> updateUI(UUID id, String tenantId, UIDefinition ui) {
        return update("ui = :ui, updatedAt = :now where id = :id and tenantId = :tenantId",
                Parameters.with("ui", ui)
                        .and("now", Instant.now())
                        .and("id", id)
                        .and("tenantId", tenantId))
                .map(count -> count > 0);
    }

    /**
     * Update validation result
     */
    public Uni<Boolean> updateValidationResult(UUID id, String tenantId, ValidationResult result) {
        Workflow.WorkflowStatus newStatus = result.isValid()
                ? Workflow.WorkflowStatus.VALID
                : Workflow.WorkflowStatus.INVALID;

        return update("validationResult = :result, status = :status, updatedAt = :now " +
                "where id = :id and tenantId = :tenantId",
                Parameters.with("result", result)
                        .and("status", newStatus)
                        .and("now", Instant.now())
                        .and("id", id)
                        .and("tenantId", tenantId))
                .map(count -> count > 0);
    }
}
