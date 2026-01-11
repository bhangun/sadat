package tech.kayys.silat.persistence.subworkflow;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sub-workflow relationship repository
 */
@ApplicationScoped
public class SubWorkflowRelationshipRepository
        implements PanacheRepositoryBase<SubWorkflowRelationshipEntity, UUID> {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(SubWorkflowRelationshipRepository.class);

    /**
     * Create relationship when child workflow is launched
     */
    public Uni<SubWorkflowRelationship> createRelationship(
            tech.kayys.silat.core.domain.WorkflowRunId parentRunId,
            tech.kayys.silat.core.domain.NodeId parentNodeId,
            tech.kayys.silat.core.domain.TenantId parentTenantId,
            tech.kayys.silat.core.domain.WorkflowRunId childRunId,
            tech.kayys.silat.core.domain.TenantId childTenantId,
            int nestingDepth,
            SubWorkflowExecutionMode executionMode) {

        return Panache.withTransaction(() -> {
            SubWorkflowRelationshipEntity entity = new SubWorkflowRelationshipEntity();
            entity.setParentRunId(parentRunId.value());
            entity.setParentNodeId(parentNodeId.value());
            entity.setParentTenantId(parentTenantId.value());
            entity.setChildRunId(childRunId.value());
            entity.setChildTenantId(childTenantId.value());
            entity.setNestingDepth(nestingDepth);
            entity.setIsCrossTenant(!parentTenantId.equals(childTenantId));
            entity.setExecutionMode(executionMode);
            entity.setStatus(RelationshipStatus.ACTIVE);

            return persist(entity)
                .map(this::toRelationship)
                .invoke(rel -> LOG.info(
                    "Created sub-workflow relationship: parent={}, child={}",
                    parentRunId.value(), childRunId.value()));
        });
    }

    /**
     * Find all child workflows for a parent
     */
    public Uni<List<SubWorkflowRelationship>> findByParent(
            tech.kayys.silat.core.domain.WorkflowRunId parentRunId,
            tech.kayys.silat.core.domain.TenantId tenantId) {

        return find(
            "parentRunId = ?1 and parentTenantId = ?2",
            parentRunId.value(),
            tenantId.value()
        ).list()
        .map(entities -> entities.stream()
            .map(this::toRelationship)
            .toList());
    }

    /**
     * Find parent workflow for a child
     */
    public Uni<SubWorkflowRelationship> findByChild(
            tech.kayys.silat.core.domain.WorkflowRunId childRunId,
            tech.kayys.silat.core.domain.TenantId tenantId) {

        return find(
            "childRunId = ?1 and childTenantId = ?2",
            childRunId.value(),
            tenantId.value()
        ).firstResult()
        .map(entity -> entity != null ? toRelationship(entity) : null);
    }

    /**
     * Update relationship status
     */
    public Uni<Void> updateStatus(
            tech.kayys.silat.core.domain.WorkflowRunId childRunId,
            RelationshipStatus status) {

        return Panache.withTransaction(() ->
            update("status = ?1, completedAt = ?2 where childRunId = ?3",
                status,
                status == RelationshipStatus.COMPLETED ? Instant.now() : null,
                childRunId.value()
            ).replaceWithVoid()
        );
    }

    /**
     * Find cross-tenant permissions
     */
    public Uni<List<tech.kayys.silat.api.subworkflow.CrossTenantPermission>> findCrossTenantPermissions(
            tech.kayys.silat.core.domain.TenantId tenantId) {

        return getEntityManager()
            .createQuery(
                "SELECT p FROM CrossTenantPermissionEntity p " +
                "WHERE (p.sourceTenantId = :tenantId OR p.targetTenantId = :tenantId) " +
                "AND p.isActive = true " +
                "AND (p.expiresAt IS NULL OR p.expiresAt > :now)",
                CrossTenantPermissionEntity.class
            )
            .setParameter("tenantId", tenantId.value())
            .setParameter("now", Instant.now())
            .getResultList()
            .map(entities -> entities.stream()
                .map(this::toPermission)
                .toList());
    }

    /**
     * Save cross-tenant permission
     */
    public Uni<tech.kayys.silat.api.subworkflow.CrossTenantPermission> saveCrossTenantPermission(
            tech.kayys.silat.api.subworkflow.CrossTenantPermission permission) {

        return Panache.withTransaction(() -> {
            CrossTenantPermissionEntity entity = new CrossTenantPermissionEntity();
            entity.setSourceTenantId(permission.sourceTenantId());
            entity.setTargetTenantId(permission.targetTenantId());
            entity.setPermissions(permission.permissions().toArray(new String[0]));
            entity.setExpiresAt(permission.expiresAt());

            return getEntityManager().persist(entity)
                .map(v -> toPermission(entity));
        });
    }

    /**
     * Delete cross-tenant permission
     */
    public Uni<Void> deleteCrossTenantPermission(
            tech.kayys.silat.core.domain.TenantId sourceTenantId,
            tech.kayys.silat.core.domain.TenantId targetTenantId) {

        return Panache.withTransaction(() ->
            getEntityManager()
                .createQuery(
                    "UPDATE CrossTenantPermissionEntity " +
                    "SET isActive = false " +
                    "WHERE sourceTenantId = :source AND targetTenantId = :target"
                )
                .setParameter("source", sourceTenantId.value())
                .setParameter("target", targetTenantId.value())
                .executeUpdate()
                .replaceWithVoid()
        );
    }

    /**
     * Audit event
     */
    public Uni<Void> auditEvent(
            tech.kayys.silat.core.domain.WorkflowRunId runId,
            tech.kayys.silat.core.domain.TenantId tenantId,
            AuditEventType eventType,
            String eventData) {

        return Panache.withTransaction(() -> {
            SubWorkflowAuditEntity audit = new SubWorkflowAuditEntity();
            audit.setRunId(runId.value());
            audit.setTenantId(tenantId.value());
            audit.setEventType(eventType);
            audit.setEventData(eventData);

            return getEntityManager().persist(audit).replaceWithVoid();
        });
    }

    // ==================== MAPPING METHODS ====================

    private SubWorkflowRelationship toRelationship(SubWorkflowRelationshipEntity entity) {
        return new SubWorkflowRelationship(
            tech.kayys.silat.core.domain.WorkflowRunId.of(entity.getParentRunId()),
            tech.kayys.silat.core.domain.NodeId.of(entity.getParentNodeId()),
            tech.kayys.silat.core.domain.TenantId.of(entity.getParentTenantId()),
            tech.kayys.silat.core.domain.WorkflowRunId.of(entity.getChildRunId()),
            tech.kayys.silat.core.domain.TenantId.of(entity.getChildTenantId()),
            entity.getNestingDepth(),
            entity.getIsCrossTenant(),
            entity.getCreatedAt()
        );
    }

    private tech.kayys.silat.api.subworkflow.CrossTenantPermission toPermission(CrossTenantPermissionEntity entity) {
        return new tech.kayys.silat.api.subworkflow.CrossTenantPermission(
            entity.getPermissionId().toString(),
            entity.getSourceTenantId(),
            entity.getTargetTenantId(),
            List.of(entity.getPermissions()),
            entity.getGrantedAt(),
            entity.getExpiresAt()
        );
    }

    private Uni<EntityManager> getEntityManager() {
        return Panache.currentSession()
            .map(session -> session.unwrap(EntityManager.class));
    }
}