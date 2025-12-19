package tech.kayys.wayang.agent.exception;

import java.util.List;

class ValidationException extends RuntimeException {
    public ValidationException(List<String> errors) {
        super("Validation failed: " + String.join(", ", errors));
    }
}
