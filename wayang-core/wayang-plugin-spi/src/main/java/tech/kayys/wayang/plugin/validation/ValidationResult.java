package tech.kayys.wayang.plugin.validation;

import java.util.ArrayList;
import java.util.List;


/**
 * Validation Result
 */
public class ValidationResult {
    
 
    private boolean valid = true;
    

    private List<ValidationError> errors = new ArrayList<>();
    
    public static ValidationResult valid() {
        return ValidationResult.builder().valid(true).build();
    }
    
    public static ValidationResult invalid(List<ValidationError> errors) {
        return ValidationResult.builder()
            .valid(false)
            .errors(errors)
            .build();
    }
    
    public void addError(String field, String message) {
        this.valid = false;
        this.errors.add(new ValidationError(field, message));
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<ValidationError> getErrors() {
        return errors;
    }

    public void setErrors(List<ValidationError> errors) {
        this.errors = errors;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean valid = true;
        private List<ValidationError> errors = new ArrayList<>();

        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder errors(List<ValidationError> errors) {
            this.errors = errors;
            return this;
        }

        public ValidationResult build() {
            ValidationResult result = new ValidationResult();
            result.valid = this.valid;
            result.errors = this.errors;
            return result;
        }
    }
}