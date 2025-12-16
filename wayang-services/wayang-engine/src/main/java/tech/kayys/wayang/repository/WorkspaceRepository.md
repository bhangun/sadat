package tech.kayys.wayang.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.domain.Workspace;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * WorkspaceRepository - Multi-tenant workspace data access
 */
@ApplicationScoped
public class WorkspaceRepository implements PanacheRepositoryBase<Workspace, UUID> {

    /**
     * Find all active workspaces for tenant
     */
    public Uni<List<Workspace>> findByTenant(String tenantId) {
        return find("tenantId = :tenantId and status = :status",
                Parameters.with("tenantId", tenantId)
                        .and("status", Workspace.WorkspaceStatus.ACTIVE))
                .list();
    }

    /**
     * Find workspace by ID with tenant validation
     */
    public Uni<Workspace> findByIdAndTenant(UUID id, String tenantId) {
        return find("id = :id and tenantId = :tenantId and status != :status",
                Parameters.with("id", id)
                        .and("tenantId", tenantId)
                        .and("status", Workspace.WorkspaceStatus.DELETED))
                .firstResult();
    }

    /**
     * Soft delete workspace
     */
    public Uni<Boolean> softDelete(UUID id, String tenantId) {
        return update("status = :status, updatedAt = :now where id = :id and tenantId = :tenantId",
                Parameters.with("status", Workspace.WorkspaceStatus.DELETED)
                        .and("now", Instant.now())
                        .and("id", id)
                        .and("tenantId", tenantId))
                .map(count -> count > 0);
    }
}
