package tech.kayys.wayang.workflow.model;

import tech.kayys.wayang.workflow.api.model.RunStatus;

/**
 * Cluster events for distributed coordination
 */
public sealed interface ClusterEvent {
    record RunCreated(String runId, String workflowId) implements ClusterEvent {
    }

    record StatusChanged(String runId, RunStatus newStatus) implements ClusterEvent {
    }

    record NodeExecuted(String runId, String nodeId) implements ClusterEvent {
    }

    record StateUpdated(String runId) implements ClusterEvent {
    }

    record RunResumed(String runId, String correlationKey) implements ClusterEvent {
    }

    record RunCancelled(String runId) implements ClusterEvent {
    }

    record RunCompleted(String runId) implements ClusterEvent {
    }
}
