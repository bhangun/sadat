package tech.kayys.wayang.mcp.plugin;

public class ValidationRule {

    private final String name;
    private final String description;
    private final ValidationSeverity severity;
    private final ValidationFunction validator;

    public ValidationRule(String name, String description, ValidationSeverity severity, ValidationFunction validator) {
        this.name = name;
        this.description = description;
        this.severity = severity;
        this.validator = validator;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public ValidationSeverity getSeverity() {
        return severity;
    }

    public ValidationFunction getValidator() {
        return validator;
    }

    public enum ValidationSeverity {
        ERROR, WARNING, INFO
    }

    @FunctionalInterface
    public interface ValidationFunction {
        ValidationResult validate(Object target, PluginExecutionContext context);
    }
}
