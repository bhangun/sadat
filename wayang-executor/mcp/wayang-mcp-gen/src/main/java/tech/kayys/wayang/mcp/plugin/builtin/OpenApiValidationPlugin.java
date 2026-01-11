package tech.kayys.wayang.mcp.plugin.builtin;

import tech.kayys.wayang.mcp.plugin.*;
import io.swagger.v3.oas.models.OpenAPI;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Arrays;
import java.util.List;

@ApplicationScoped
public class OpenApiValidationPlugin implements ValidationPlugin {

    @Override
    public String getValidationType() {
        return "openapi";
    }

    @Override
    public void initialize() throws PluginException {
        // Initialization logic
    }

    @Override
    public boolean supports(String validationType) {
        return "openapi".equalsIgnoreCase(validationType) ||
                "swagger".equalsIgnoreCase(validationType);
    }

    @Override
    public ValidationResult validate(Object target, PluginExecutionContext context) throws PluginException {
        if (!(target instanceof OpenAPI)) {
            return new ValidationResult(false).addError("Target is not an OpenAPI specification");
        }

        OpenAPI openAPI = (OpenAPI) target;
        ValidationResult result = new ValidationResult(true);

        // Validate info section
        if (openAPI.getInfo() == null) {
            result.addError("OpenAPI specification must have an 'info' section");
        } else {
            if (openAPI.getInfo().getTitle() == null || openAPI.getInfo().getTitle().trim().isEmpty()) {
                result.addWarning("OpenAPI info.title is missing or empty");
            }
            if (openAPI.getInfo().getVersion() == null || openAPI.getInfo().getVersion().trim().isEmpty()) {
                result.addWarning("OpenAPI info.version is missing or empty");
            }
        }

        // Validate paths
        if (openAPI.getPaths() == null || openAPI.getPaths().isEmpty()) {
            result.addError("OpenAPI specification must have at least one path defined");
        }

        return result;
    }

    @Override
    public List<ValidationRule> getValidationRules() {
        return Arrays.asList(
                new ValidationRule("info-required", "Info section is required",
                        ValidationRule.ValidationSeverity.ERROR, this::validateInfo),
                new ValidationRule("paths-required", "At least one path is required",
                        ValidationRule.ValidationSeverity.ERROR, this::validatePaths),
                new ValidationRule("title-recommended", "Title should be provided",
                        ValidationRule.ValidationSeverity.WARNING, this::validateTitle));
    }

    private ValidationResult validateInfo(Object target, PluginExecutionContext context) {
        OpenAPI openAPI = (OpenAPI) target;
        ValidationResult result = new ValidationResult(openAPI.getInfo() != null);
        if (openAPI.getInfo() == null) {
            result.addError("Info section is required");
        }
        return result;
    }

    private ValidationResult validatePaths(Object target, PluginExecutionContext context) {
        OpenAPI openAPI = (OpenAPI) target;
        boolean hasPaths = openAPI.getPaths() != null && !openAPI.getPaths().isEmpty();
        ValidationResult result = new ValidationResult(hasPaths);
        if (!hasPaths) {
            result.addError("At least one path must be defined");
        }
        return result;
    }

    private ValidationResult validateTitle(Object target, PluginExecutionContext context) {
        OpenAPI openAPI = (OpenAPI) target;
        ValidationResult result = new ValidationResult(true);
        if (openAPI.getInfo() != null &&
                (openAPI.getInfo().getTitle() == null || openAPI.getInfo().getTitle().trim().isEmpty())) {
            result.addWarning("API title should be provided");
        }
        return result;
    }
}
