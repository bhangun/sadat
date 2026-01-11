package tech.kayys.silat.enhancements;

import io.smallrye.mutiny.Uni;
import java.util.Map;

/**
 * Enhancement: Chaos Engineering for Workflows
 */
interface WorkflowChaosService {

    /**
     * Inject failure into node
     */
    Uni<Void> injectFailure(
        tech.kayys.silat.core.domain.WorkflowRunId runId,
        tech.kayys.silat.core.domain.NodeId nodeId,
        FailureType type,
        java.time.Duration duration
    );

    /**
     * Inject latency
     */
    Uni<Void> injectLatency(
        tech.kayys.silat.core.domain.WorkflowRunId runId,
        tech.kayys.silat.core.domain.NodeId nodeId,
        java.time.Duration latency
    );

    /**
     * Simulate network partition
     */
    Uni<Void> simulateNetworkPartition(
        List<String> isolatedNodes,
        java.time.Duration duration
    );

    /**
     * Run chaos experiment
     */
    Uni<ChaosResult> runExperiment(
        ChaosExperiment experiment
    );
}