package tech.kayys.wayang.workflow.exception;

/**
 * Exception for checkpoint not found
 */
public class CheckpointNotFoundException extends RuntimeException {
    public CheckpointNotFoundException(String message) {
        super(message);
    }
}