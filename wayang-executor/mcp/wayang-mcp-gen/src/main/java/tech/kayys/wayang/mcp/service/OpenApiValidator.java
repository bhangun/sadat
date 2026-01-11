package tech.kayys.wayang.mcp.service;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;

import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class OpenApiValidator {

    public ValidationResult validate(OpenAPI openAPI) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Validate info section
        validateInfo(openAPI, errors, warnings);

        // Validate paths
        validatePaths(openAPI, errors, warnings);

        // Validate operations
        validateOperations(openAPI, errors, warnings);

        // Validate components
        validateComponents(openAPI, errors, warnings);

        return new ValidationResult(errors, warnings);
    }

    private void validateInfo(OpenAPI openAPI, List<String> errors, List<String> warnings) {
        if (openAPI.getInfo() == null) {
            errors.add("OpenAPI specification must have an 'info' section");
            return;
        }

        if (StringUtils.isBlank(openAPI.getInfo().getTitle())) {
            warnings.add("OpenAPI info.title is missing or empty");
        }

        if (StringUtils.isBlank(openAPI.getInfo().getVersion())) {
            warnings.add("OpenAPI info.version is missing or empty");
        }
    }

    private void validatePaths(OpenAPI openAPI, List<String> errors, List<String> warnings) {
        if (openAPI.getPaths() == null || openAPI.getPaths().isEmpty()) {
            errors.add("OpenAPI specification must have at least one path defined");
            return;
        }

        // Check for duplicate operation IDs
        Set<String> operationIds = new HashSet<>();
        openAPI.getPaths().forEach((path, pathItem) -> {
            if (pathItem.readOperationsMap() != null) {
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    if (operation.getOperationId() != null) {
                        if (operationIds.contains(operation.getOperationId())) {
                            errors.add("Duplicate operationId found: " + operation.getOperationId());
                        } else {
                            operationIds.add(operation.getOperationId());
                        }
                    }
                });
            }
        });
    }

    private void validateOperations(OpenAPI openAPI, List<String> errors, List<String> warnings) {
        if (openAPI.getPaths() != null) {
            openAPI.getPaths().forEach((path, pathItem) -> {
                if (pathItem.readOperationsMap() != null) {
                    pathItem.readOperationsMap().forEach((method, operation) -> {
                        validateOperation(path, method.name(), operation, warnings);
                    });
                }
            });
        }
    }

    private void validateOperation(String path, String method, Operation operation, List<String> warnings) {
        if (StringUtils.isBlank(operation.getSummary()) && StringUtils.isBlank(operation.getDescription())) {
            warnings.add("Operation " + method + " " + path + " has no summary or description");
        }

        if (operation.getResponses() == null || operation.getResponses().isEmpty()) {
            warnings.add("Operation " + method + " " + path + " has no response definitions");
        }
    }

    private void validateComponents(OpenAPI openAPI, List<String> errors, List<String> warnings) {
        if (openAPI.getComponents() != null) {
            if (openAPI.getComponents().getSecuritySchemes() != null) {
                warnings.add(
                        "Security schemes are defined but may require additional configuration in generated MCP server");
            }
        }
    }

    public static class ValidationResult {
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(List<String> errors, List<String> warnings) {
            this.errors = errors != null ? errors : new ArrayList<>();
            this.warnings = warnings != null ? warnings : new ArrayList<>();
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }
}