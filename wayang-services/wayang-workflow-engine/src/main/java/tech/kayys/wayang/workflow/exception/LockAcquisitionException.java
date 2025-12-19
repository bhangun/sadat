package tech.kayys.wayang.workflow.exception;

/**
 * Lock acquisition exception
 */
public class LockAcquisitionException extends RuntimeException {
    public LockAcquisitionException(String message) {
        super(message);
    }
}
