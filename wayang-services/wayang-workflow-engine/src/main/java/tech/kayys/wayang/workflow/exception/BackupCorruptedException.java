package tech.kayys.wayang.workflow.exception;

public class BackupCorruptedException extends RuntimeException {
    public BackupCorruptedException(String message) {
        super(message);
    }
}
