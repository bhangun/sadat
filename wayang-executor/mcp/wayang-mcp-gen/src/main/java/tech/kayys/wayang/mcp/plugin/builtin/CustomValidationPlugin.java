package tech.kayys.wayang.mcp.plugin.builtin;

import tech.kayys.wayang.mcp.model.McpServerModel;
import tech.kayys.wayang.mcp.plugin.*;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@ApplicationScoped
public class CustomValidationPlugin implements ValidationPlugin {

    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)*$");
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("^[A-Z][a-zA-Z0-9_]*$");

    @Override
    public String getValidationType() {
        return "mcp-server-model";
    }

    @Override
    public void initialize() throws PluginException {
        // Initialization logic
    }

    @Override
    public boolean supports(String validationType) {
        return "mcp-server-model".equalsIgnoreCase(validationType) ||
                "server-model".equalsIgnoreCase(validationType);
    }

    @Override
    public ValidationResult validate(Object target, PluginExecutionContext context) throws PluginException {
        if (!(target instanceof McpServerModel)) {
            return new ValidationResult(false).addError("Target is not a McpServerModel");
        }

        McpServerModel model = (McpServerModel) target;
        ValidationResult result = new ValidationResult(true);

        // Validate package name
        if (model.getPackageName() == null || !PACKAGE_NAME_PATTERN.matcher(model.getPackageName()).matches()) {
            result.addError("Invalid package name format: " + model.getPackageName());
        }

        // Validate server name
        if (model.getServerName() == null || !CLASS_NAME_PATTERN.matcher(model.getServerName()).matches()) {
            result.addError("Invalid server name format: " + model.getServerName());
        }

        // Validate tools
        if (model.getTools() == null || model.getTools().isEmpty()) {
            result.addWarning("No tools defined in the server model");
        } else {
            // Check for duplicate tool names
            List<String> toolNames = model.getTools().stream()
                    .map(tool -> tool.getName())
                    .toList();

            Set<String> uniqueNames = new HashSet<>(toolNames);
            if (uniqueNames.size() != toolNames.size()) {
                result.addError("Duplicate tool names detected");
            }
        }

        // Validate base URL
        if (model.getBaseUrl() != null) {
            try {
                new java.net.URL(model.getBaseUrl());
            } catch (Exception e) {
                result.addWarning("Base URL may not be valid: " + model.getBaseUrl());
            }
        }

        return result;
    }

    @Override
    public List<ValidationRule> getValidationRules() {
        return Arrays.asList(
                new ValidationRule("package-name-valid", "Package name must be valid Java package name",
                        ValidationRule.ValidationSeverity.ERROR, this::validatePackageName),
                new ValidationRule("server-name-valid", "Server name must be valid Java class name",
                        ValidationRule.ValidationSeverity.ERROR, this::validateServerName),
                new ValidationRule("tools-present", "At least one tool should be defined",
                        ValidationRule.ValidationSeverity.WARNING, this::validateToolsPresent),
                new ValidationRule("base-url-valid", "Base URL should be a valid URL",
                        ValidationRule.ValidationSeverity.WARNING, this::validateBaseUrl));
    }

    private ValidationResult validatePackageName(Object target, PluginExecutionContext context) {
        McpServerModel model = (McpServerModel) target;
        boolean isValid = model.getPackageName() != null &&
                PACKAGE_NAME_PATTERN.matcher(model.getPackageName()).matches();
        ValidationResult result = new ValidationResult(isValid);
        if (!isValid) {
            result.addError("Invalid package name: " + model.getPackageName());
        }
        return result;
    }

    private ValidationResult validateServerName(Object target, PluginExecutionContext context) {
        McpServerModel model = (McpServerModel) target;
        boolean isValid = model.getServerName() != null &&
                CLASS_NAME_PATTERN.matcher(model.getServerName()).matches();
        ValidationResult result = new ValidationResult(isValid);
        if (!isValid) {
            result.addError("Invalid server name: " + model.getServerName());
        }
        return result;
    }

    private ValidationResult validateToolsPresent(Object target, PluginExecutionContext context) {
        McpServerModel model = (McpServerModel) target;
        ValidationResult result = new ValidationResult(true);
        if (model.getTools() == null || model.getTools().isEmpty()) {
            result.addWarning("No tools defined in server model");
        }
        return result;
    }

    private ValidationResult validateBaseUrl(Object target, PluginExecutionContext context) {
        McpServerModel model = (McpServerModel) target;
        ValidationResult result = new ValidationResult(true);
        if (model.getBaseUrl() != null) {
            try {
                new java.net.URL(model.getBaseUrl());
            } catch (Exception e) {
                result.addWarning("Base URL may not be valid: " + model.getBaseUrl());
            }
        }
        return result;
    }
}
