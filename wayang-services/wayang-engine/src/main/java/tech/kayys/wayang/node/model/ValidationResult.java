package tech.kayys.wayang.node.model;

import java.util.List;

/**
 * Validation result.
 */
@lombok.Data
@lombok.AllArgsConstructor
class ValidationResult {
    private boolean valid;
    private List<String> errors;

    public static ValidationResult valid() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(false, errors);
    }
}
