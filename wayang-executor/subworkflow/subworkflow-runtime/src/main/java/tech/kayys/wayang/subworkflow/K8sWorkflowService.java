package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Kubernetes Native Integration
 */
interface K8sWorkflowService {

    /**
     * Deploy workflow as Kubernetes CRD
     */
    Uni<Void> deployAsCRD(
        tech.kayys.silat.core.domain.WorkflowDefinition workflow,
        String namespace
    );

    /**
     * Scale executors based on load
     */
    Uni<Void> autoScale(
        ScalingPolicy policy
    );

    /**
     * Deploy to Kubernetes cluster
     */
    Uni<Deployment> deployToK8s(
        tech.kayys.silat.core.domain.WorkflowDefinitionId workflowId,
        K8sDeploymentConfig config
    );
}