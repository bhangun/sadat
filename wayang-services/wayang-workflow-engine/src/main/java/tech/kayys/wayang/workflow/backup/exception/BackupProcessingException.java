package tech.kayys.wayang.workflow.backup.exception;

public class BackupProcessingException extends RuntimeException {
    public BackupProcessingException(String message) {
        super(message);
    }

    public BackupProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}