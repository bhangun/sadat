package tech.kayys.silat.persistence.subworkflow;

import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Sub-workflow relationship domain object
 */
record SubWorkflowRelationship(
    tech.kayys.silat.core.domain.WorkflowRunId parentRunId,
    tech.kayys.silat.core.domain.NodeId parentNodeId,
    tech.kayys.silat.core.domain.TenantId parentTenantId,
    tech.kayys.silat.core.domain.WorkflowRunId childRunId,
    tech.kayys.silat.core.domain.TenantId childTenantId,
    int nestingDepth,
    boolean isCrossTenant,
    java.time.Instant createdAt
) {}