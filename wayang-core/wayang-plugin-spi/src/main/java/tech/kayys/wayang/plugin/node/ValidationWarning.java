package tech.kayys.wayang.plugin.node;

/**
 * Minimal ValidationWarning shim used by validators.
 */
public class ValidationWarning {
    private final String field;
    private final String code;
    private final String message;

    public ValidationWarning(String field, String code, String message) {
        this.field = field;
        this.code = code;
        this.message = message;
    }

    public String getField() { return field; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
}
package tech.kayys.wayang.plugin.node;

/**
 * Validation warning
 */
record ValidationWarning(
    String field,
    String code,
    String message
) {}