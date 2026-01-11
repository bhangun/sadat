package tech.kayys.wayang.mcp.plugin;

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {

    private final boolean valid;
    private final List<ValidationIssue> issues = new ArrayList<>();

    public ValidationResult(boolean valid) {
        this.valid = valid;
    }

    public ValidationResult addIssue(ValidationIssue issue) {
        this.issues.add(issue);
        return this;
    }

    public ValidationResult addError(String message) {
        return addIssue(new ValidationIssue(ValidationRule.ValidationSeverity.ERROR, message));
    }

    public ValidationResult addWarning(String message) {
        return addIssue(new ValidationIssue(ValidationRule.ValidationSeverity.WARNING, message));
    }

    public boolean isValid() {
        return valid;
    }

    public List<ValidationIssue> getIssues() {
        return new ArrayList<>(issues);
    }

    public List<ValidationIssue> getErrors() {
        return issues.stream()
                .filter(issue -> issue.getSeverity() == ValidationRule.ValidationSeverity.ERROR)
                .toList();
    }

    public List<ValidationIssue> getWarnings() {
        return issues.stream()
                .filter(issue -> issue.getSeverity() == ValidationRule.ValidationSeverity.WARNING)
                .toList();
    }

    public static class ValidationIssue {
        private final ValidationRule.ValidationSeverity severity;
        private final String message;
        private final String field;

        public ValidationIssue(ValidationRule.ValidationSeverity severity, String message) {
            this(severity, message, null);
        }

        public ValidationIssue(ValidationRule.ValidationSeverity severity, String message, String field) {
            this.severity = severity;
            this.message = message;
            this.field = field;
        }

        public ValidationRule.ValidationSeverity getSeverity() {
            return severity;
        }

        public String getMessage() {
            return message;
        }

        public String getField() {
            return field;
        }
    }
}
