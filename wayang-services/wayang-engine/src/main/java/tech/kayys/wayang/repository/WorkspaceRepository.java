package tech.kayys.wayang.repository;

import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.domain.Workspace;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
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

        /**
         * Find workspace by ID with tenant validation
         */
        public Uni<Optional<Workspace>> findByIdAndTenant(UUID id, UUID tenantId) {
                return find("id = :id and tenantId = :tenantId and status != :deleted",
                                Parameters.with("id", id)
                                                .and("tenantId", tenantId)
                                                .and("deleted", Workspace.WorkspaceStatus.DELETED))
                                .firstResultOptional();
        }

        /**
         * List all workspaces for a tenant
         */
        public Uni<List<Workspace>> findByTenant(UUID tenantId, int page, int size) {
                return find("tenantId = :tenantId and status != :deleted order by updatedAt desc",
                                Parameters.with("tenantId", tenantId)
                                                .and("deleted", Workspace.WorkspaceStatus.DELETED))
                                .page(page, size)
                                .list();
        }

        /**
         * Find workspaces by owner
         */
        public Uni<List<Workspace>> findByOwner(UUID tenantId, String ownerId, int page, int size) {
                return find("tenantId = :tenantId and ownerId = :ownerId and status != :deleted order by updatedAt desc",
                                Parameters.with("tenantId", tenantId)
                                                .and("ownerId", ownerId)
                                                .and("deleted", Workspace.WorkspaceStatus.DELETED))
                                .page(page, size)
                                .list();
        }

        /**
         * Search workspaces by name
         */
        public Uni<List<Workspace>> searchByName(UUID tenantId, String namePattern, int page, int size) {
                return find("tenantId = :tenantId and lower(name) like :pattern and status != :deleted order by name",
                                Parameters.with("tenantId", tenantId)
                                                .and("pattern", "%" + namePattern.toLowerCase() + "%")
                                                .and("deleted", Workspace.WorkspaceStatus.DELETED))
                                .page(page, size)
                                .list();
        }

        /**
         * Count workspaces by tenant
         */
        public Uni<Long> countByTenant(UUID tenantId) {
                return count("tenantId = :tenantId and status != :deleted",
                                Parameters.with("tenantId", tenantId)
                                                .and("deleted", Workspace.WorkspaceStatus.DELETED));
        }

        /**
         * Check if workspace name exists for tenant
         */
        public Uni<Boolean> existsByNameAndTenant(String name, UUID tenantId) {
                return count("name = :name and tenantId = :tenantId and status != :deleted",
                                Parameters.with("name", name)
                                                .and("tenantId", tenantId)
                                                .and("deleted", Workspace.WorkspaceStatus.DELETED))
                                .map(count -> count > 0);
        }

        /**
         * Soft delete workspace
         */
        public Uni<Void> softDelete(UUID id, UUID tenantId) {
                return update("status = :deleted, updatedAt = current_timestamp where id = :id and tenantId = :tenantId",
                                Parameters.with("deleted", Workspace.WorkspaceStatus.DELETED)
                                                .and("id", id)
                                                .and("tenantId", tenantId))
                                .replaceWithVoid();
        }

        /**
         * Find workspaces with workflow count
         */
        public Uni<List<Object[]>> findWithWorkflowCount(UUID tenantId) {
                return getEntityManager()
                                .createQuery(
                                                "SELECT w, COUNT(wf) FROM Workspace w LEFT JOIN w.workflows wf " +
                                                                "WHERE w.tenantId = :tenantId AND w.status != :deleted "
                                                                +
                                                                "GROUP BY w ORDER BY w.updatedAt DESC",
                                                Object[].class)
                                .setParameter("tenantId", tenantId)
                                .setParameter("deleted", Workspace.WorkspaceStatus.DELETED)
                                .getResultList();
        }
}
