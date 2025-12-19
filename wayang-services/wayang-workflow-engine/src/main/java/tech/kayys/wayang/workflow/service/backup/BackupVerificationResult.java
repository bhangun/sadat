package tech.kayys.wayang.workflow.service.backup;

/**
 * Result of backup verification
 */
public class BackupVerificationResult {
    private final boolean valid;
    private final java.util.List<String> issues;

    public BackupVerificationResult(boolean valid, java.util.List<String> issues) {
        this.valid = valid;
        this.issues = issues;
    }

    public boolean isValid() {
        return valid;
    }

    public java.util.List<String> getIssues() {
        return issues;
    }

    public static BackupVerificationResult success() {
        return new BackupVerificationResult(true, java.util.List.of());
    }

    public static BackupVerificationResult failure(java.util.List<String> issues) {
        return new BackupVerificationResult(false, issues);
    }
}
