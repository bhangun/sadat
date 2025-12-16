package tech.kayys.wayang.plugin.validation;

/**
 * Validation Error
 */
public class ValidationError {
    private String field;
    private String message;

    public ValidationError() {}

    public ValidationError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ValidationError instance = new ValidationError();

        public Builder field(String field) {
            instance.field = field;
            return this;
        }

        public Builder message(String message) {
            instance.message = message;
            return this;
        }

        public ValidationError build() {
            return instance;
        }
    }
}
