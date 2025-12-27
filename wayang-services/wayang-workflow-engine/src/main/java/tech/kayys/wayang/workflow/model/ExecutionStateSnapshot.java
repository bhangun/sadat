package tech.kayys.wayang.workflow.model;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.kayys.wayang.workflow.saga.model.CompensationSnapshot;
import tech.kayys.wayang.workflow.saga.model.SagaExecutionSnapshot;

/**
 * Execution state snapshot at checkpoint time
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionStateSnapshot {

    /**
     * Current execution position
     */
    private String currentNodeId;
    private List<String> executedNodes;
    private List<String> pendingNodes;

    /**
     * Node execution records
     */
    private Map<String, NodeExecutionSnapshot> nodeRecords;

    /**
     * Workflow variables
     */
    private Map<String, Object> variables;

    /**
     * Execution context
     */
    private Map<String, Object> context;

    /**
     * Human tasks state
     */
    private List<HumanTaskSnapshot> pendingHumanTasks;

    /**
     * Error recovery state
     */
    private ErrorRecoverySnapshot errorRecovery;

    /**
     * Saga execution state (if using saga pattern)
     */
    private SagaExecutionSnapshot sagaState;

    /**
     * Compensation state
     */
    private CompensationSnapshot compensationState;
}
