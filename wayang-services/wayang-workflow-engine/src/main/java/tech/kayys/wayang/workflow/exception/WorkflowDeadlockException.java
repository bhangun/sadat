package tech.kayys.wayang.workflow.exception;

public class WorkflowDeadlockException extends RuntimeException {
    public WorkflowDeadlockException(String message) {
        super(message);
    }
}
