package tech.kayys.wayang.workflow.kernel;

/**
 * Status of a node execution.
 */
public enum NodeExecutionStatus {
    PENDING, // Waiting to execute
    EXECUTING, // Currently executing
    COMPLETED, // Successfully completed
    FAILED, // Execution failed
    WAITING, // Waiting for external signal
    CANCELLED, // Execution cancelled
    SKIPPED, // Skipped due to conditions
    RETRYING // Currently retrying
    , SUCCESS
}
