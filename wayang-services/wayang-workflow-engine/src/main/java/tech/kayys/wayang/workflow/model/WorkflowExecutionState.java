package tech.kayys.wayang.workflow.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Workflow execution state - captures current execution progress
 */
@Data
@NoArgsConstructor
public class WorkflowExecutionState {

    /**
     * Current node being executed
     */
    private String currentNodeId;

    /**
     * Execution path taken
     */
    private List<String> executedNodes = new ArrayList<>();

    /**
     * Node execution history
     */
    private Map<String, NodeExecutionRecord> nodeRecords = new HashMap<>();

    /**
     * Workflow-level variables
     */
    private Map<String, Object> variables = new HashMap<>();

    /**
     * Pending human tasks
     */
    private List<String> pendingHumanTasks = new ArrayList<>();

    /**
     * Error recovery state
     */
    private ErrorRecoveryState errorRecovery;

    /**
     * Checkpoint for recovery
     */
    private String lastCheckpointId;

    private Instant lastCheckpointAt;
}
