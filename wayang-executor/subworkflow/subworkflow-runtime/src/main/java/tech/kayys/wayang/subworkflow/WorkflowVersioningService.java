package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Workflow Versioning & Blue-Green Deployments
 */
interface WorkflowVersioningService {

    /**
     * Create new version of workflow
     */
    Uni<WorkflowVersion> createVersion(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        String version,
        VersionType type // MAJOR, MINOR, PATCH
    );

    /**
     * Deploy version with traffic splitting
     */
    Uni<Deployment> deployVersion(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        String version,
        TrafficSplit split // e.g., 90% old, 10% new
    );

    /**
     * Rollback to previous version
     */
    Uni<Void> rollback(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        String toVersion
    );

    /**
     * Compare two versions
     */
    Uni<VersionComparison> compareVersions(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        String version1,
        String version2
    );
}