package tech.kayys.wayang.agent.exception;

public class WorkflowExecutionException extends RuntimeException {
    public WorkflowExecutionException(String message) {
        super("Workflow execution failed: " + message);
    }
    
    public WorkflowExecutionException(String message, Throwable cause) {
        super("Workflow execution failed: " + message, cause);
    }
}