package tech.kayys.wayang.node.model;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import tech.kayys.wayang.schema.execution.ErrorPayload;

/**
 * Checkpoint data for persistence.
 */
@Data
@Builder
class CheckpointData {
    private Map<String, NodeExecutionResult> nodeResults;
    private Map<String, NodeState> nodeStates;
    private Map<String, Object> dataFlow;
    private List<ErrorPayload> errorHistory;
    private List<HTILTaskResult> humanDecisions;
    private Map<String, Object> metadata;
    private boolean awaitingHuman;
    private String humanTaskId;
}