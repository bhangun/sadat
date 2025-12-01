package tech.kayys.wayang.node.core.exception;

import tech.kayys.wayang.node.core.validation.ValidationResult;

/**
 * Exception thrown when validation fails.
 */
public class ValidationException extends NodeException {
    
    private final ValidationResult validationResult;
    
    public ValidationException(String message, ValidationResult validationResult) {
        super(message, "VALIDATION_ERROR", false);
        this.validationResult = validationResult;
    }
    
    public ValidationResult getValidationResult() {
        return validationResult;
    }
    
    /**
     * Get formatted error message with all validation errors
     */
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(super.getMessage());
        sb.append("\nValidation errors:");
        
        if (validationResult != null) {
            for (var error : validationResult.errors()) {
                sb.append("\n  - ").append(error.field())
                  .append(": ").append(error.message());
            }
        }
        
        return sb.toString();
    }
}