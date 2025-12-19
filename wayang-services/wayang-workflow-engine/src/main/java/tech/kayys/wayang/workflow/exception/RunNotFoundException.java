package tech.kayys.wayang.workflow.exception;

/**
 * Run not found exception
 */
public class RunNotFoundException extends WayangException {
    public RunNotFoundException(String message) {
        super("RUN_NOT_FOUND", message);
    }
}
