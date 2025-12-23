package tech.kayys.wayang.workflow.service.backup;

/**
 * Exception thrown when backup verification fails
 */
public class BackupVerificationException extends RuntimeException {
    public BackupVerificationException(String message) {
        super(message);
    }

    public BackupVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
