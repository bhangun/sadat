package tech.kayys.wayang.plugin.node;

import java.util.List;

/**
 * Validation result
 */
record ValidationResult(
    boolean valid,
    List<ValidationError> errors,
    List<ValidationWarning> warnings
) {
    public static ValidationResult success() {
        return new ValidationResult(true, List.of(), List.of());
    }
    
    public static ValidationResult failure(List<ValidationError> errors) {
        return new ValidationResult(false, errors, List.of());
    }
    
    public static ValidationResult withWarnings(List<ValidationWarning> warnings) {
        return new ValidationResult(true, List.of(), warnings);
    }
}