package tech.kayys.wayang.workflow.service.backup;

public class BackupProcessingException extends RuntimeException {
    public BackupProcessingException(String message) {
        super(message);
    }

    public BackupProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}