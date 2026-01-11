package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: GitOps Workflow Management
 */
interface GitOpsWorkflowService {

    /**
     * Sync workflows from Git repository
     */
    Uni<Void> syncFromGit(
        String repositoryUrl,
        String branch,
        tech.kayys.silat.core.domain.TenantId tenantId
    );

    /**
     * Deploy from Git tag/commit
     */
    Uni<Deployment> deployFromGit(
        String repositoryUrl,
        String ref, // tag or commit SHA
        tech.kayys.silat.core.domain.TenantId tenantId
    );

    /**
     * Enable auto-sync
     */
    Uni<Void> enableAutoSync(
        String repositoryUrl,
        java.time.Duration syncInterval
    );
}