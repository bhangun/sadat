package tech.kayys.wayang.workflow.exception;

class WorkflowNotFoundException extends RuntimeException {
    public WorkflowNotFoundException(String message) {
        super(message);
    }
}
