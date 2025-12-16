package tech.kayys.wayang.plugin.node;

/**
 * Minimal ValidationError shim used by validators.
 */
public class ValidationError {
    private final String field;
    private final String code;
    private final String message;
    private final Object details;

    public ValidationError(String field, String code, String message, Object details) {
        this.field = field;
        this.code = code;
        this.message = message;
        this.details = details;
    }

    public String getField() { return field; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public Object getDetails() { return details; }
}
package tech.kayys.wayang.plugin.node;

/**
 * Validation error
 */
record ValidationError(
    String field,
    String code,
    String message,
    Object rejectedValue
) {}
