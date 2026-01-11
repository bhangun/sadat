package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Multi-Cloud Support
 */
interface MultiCloudService {

    /**
     * Deploy to multiple clouds
     */
    Uni<List<Deployment>> deployMultiCloud(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        List<CloudProvider> providers
    );

    /**
     * Load balance across clouds
     */
    Uni<Void> configureLoadBalancing(
        LoadBalancingStrategy strategy
    );

    /**
     * Cloud cost optimization
     */
    Uni<CostReport> optimizeCloudCosts(
        tech.kayys.silat.core.domain.TenantId tenantId,
        java.time.Duration period
    );
}