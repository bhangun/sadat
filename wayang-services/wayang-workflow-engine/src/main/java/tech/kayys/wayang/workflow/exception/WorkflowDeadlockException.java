package tech.kayys.wayang.workflow.exception;

class WorkflowDeadlockException extends RuntimeException {
    public WorkflowDeadlockException(String message) {
        super(message);
    }
}
