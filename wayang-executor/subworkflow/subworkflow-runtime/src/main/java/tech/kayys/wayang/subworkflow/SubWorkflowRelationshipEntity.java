package tech.kayys.silat.persistence.subworkflow;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import tech.kayys.silat.api.subworkflow.CrossTenantPermission;
import tech.kayys.silat.core.domain.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sub-workflow relationship entity
 * Tracks parent-child workflow relationships
 */
@Entity
@Table(
    name = "sub_workflow_relationships",
    indexes = {
        @Index(name = "idx_parent_run", columnList = "parent_run_id"),
        @Index(name = "idx_child_run", columnList = "child_run_id"),
        @Index(name = "idx_parent_tenant", columnList = "parent_tenant_id"),
        @Index(name = "idx_child_tenant", columnList = "child_tenant_id"),
        @Index(name = "idx_created_at", columnList = "created_at")
    }
)
public class SubWorkflowRelationshipEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "relationship_id")
    private UUID relationshipId;

    /**
     * Parent workflow information
     */
    @Column(name = "parent_run_id", nullable = false)
    private String parentRunId;

    @Column(name = "parent_node_id", nullable = false)
    private String parentNodeId;

    @Column(name = "parent_tenant_id", nullable = false)
    private String parentTenantId;

    @Column(name = "parent_definition_id")
    private String parentDefinitionId;

    /**
     * Child workflow information
     */
    @Column(name = "child_run_id", nullable = false, unique = true)
    private String childRunId;

    @Column(name = "child_tenant_id", nullable = false)
    private String childTenantId;

    @Column(name = "child_definition_id")
    private String childDefinitionId;

    /**
     * Relationship metadata
     */
    @Column(name = "nesting_depth")
    private Integer nestingDepth;

    @Column(name = "is_cross_tenant")
    private Boolean isCrossTenant;

    @Column(name = "execution_mode")
    @Enumerated(EnumType.STRING)
    private SubWorkflowExecutionMode executionMode;

    /**
     * Audit fields
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private RelationshipStatus status;

    // Getters and setters
    public UUID getRelationshipId() { return relationshipId; }
    public void setRelationshipId(UUID relationshipId) {
        this.relationshipId = relationshipId;
    }

    public String getParentRunId() { return parentRunId; }
    public void setParentRunId(String parentRunId) {
        this.parentRunId = parentRunId;
    }

    public String getParentNodeId() { return parentNodeId; }
    public void setParentNodeId(String parentNodeId) {
        this.parentNodeId = parentNodeId;
    }

    public String getParentTenantId() { return parentTenantId; }
    public void setParentTenantId(String parentTenantId) {
        this.parentTenantId = parentTenantId;
    }

    public String getChildRunId() { return childRunId; }
    public void setChildRunId(String childRunId) {
        this.childRunId = childRunId;
    }

    public String getChildTenantId() { return childTenantId; }
    public void setChildTenantId(String childTenantId) {
        this.childTenantId = childTenantId;
    }

    public Integer getNestingDepth() { return nestingDepth; }
    public void setNestingDepth(Integer nestingDepth) {
        this.nestingDepth = nestingDepth;
    }

    public Boolean getIsCrossTenant() { return isCrossTenant; }
    public void setIsCrossTenant(Boolean isCrossTenant) {
        this.isCrossTenant = isCrossTenant;
    }

    public SubWorkflowExecutionMode getExecutionMode() { return executionMode; }
    public void setExecutionMode(SubWorkflowExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public RelationshipStatus getStatus() { return status; }
    public void setStatus(RelationshipStatus status) {
        this.status = status;
    }
}