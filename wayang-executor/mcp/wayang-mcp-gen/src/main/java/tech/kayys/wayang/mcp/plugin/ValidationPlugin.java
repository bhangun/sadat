package tech.kayys.wayang.mcp.plugin;

import java.util.List;

public interface ValidationPlugin {

    String getValidationType();

    void initialize() throws PluginException;

    boolean supports(String validationType);

    ValidationResult validate(Object target, PluginExecutionContext context) throws PluginException;

    List<ValidationRule> getValidationRules();
}
