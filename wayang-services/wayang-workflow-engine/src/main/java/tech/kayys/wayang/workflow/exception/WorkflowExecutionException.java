package tech.kayys.wayang.workflow.exception;

/**
 * Workflow execution exception
 */
class WorkflowExecutionException extends WayangException {
    public WorkflowExecutionException(String message) {
        super("WORKFLOW_EXECUTION_ERROR", message);
    }

    public WorkflowExecutionException(String message, Throwable cause) {
        super("WORKFLOW_EXECUTION_ERROR", message, cause);
    }
}