package tech.kayys.wayang.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ValidationResult - Workflow validation outcome
 */
public class ValidationResult {
    public boolean valid = true;
    public Instant validatedAt;
    public String validatorVersion;
    public List<ValidationError> errors = new ArrayList<>();
    public List<ValidationWarning> warnings = new ArrayList<>();

    public boolean isValid() {
        return valid && (errors == null || errors.isEmpty());
    }

    public static class ValidationError {
        public String code;
        public String message;
        public String nodeId;
        public String path;
        public ErrorSeverity severity = ErrorSeverity.ERROR;

        public enum ErrorSeverity {
            ERROR, WARNING, INFO
        }
    }

    public static class ValidationWarning {
        public String message;
        public String nodeId;
        public String suggestion;
    }
}
