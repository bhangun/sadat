package tech.kayys.wayang.workflow.exception;

/**
 * Exception for not found runs.
 */
public class WorkflowRunNotFoundException extends RuntimeException {
    public WorkflowRunNotFoundException(String message) {
        super(message);
    }
}