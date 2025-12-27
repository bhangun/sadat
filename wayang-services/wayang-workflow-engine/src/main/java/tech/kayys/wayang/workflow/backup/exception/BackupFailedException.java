package tech.kayys.wayang.workflow.backup.exception;

public class BackupFailedException extends RuntimeException {
    public BackupFailedException(String message) {
        super(message);
    }

    public BackupFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}