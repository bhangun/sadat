package tech.kayys.wayang.sdk.util;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import tech.kayys.wayang.sdk.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client-side validation utilities for workflow requests
 */
@ApplicationScoped
public class WorkflowValidator {

    @Inject
    ObjectMapper objectMapper;

    /**
     * Validate trigger request
     */
    public ValidationResult validateTriggerRequest(TriggerWorkflowRequest request) {
        List<String> errors = new ArrayList<>();

        if (request.workflowId() == null || request.workflowId().isBlank()) {
            errors.add("workflowId is required");
        }

        if (request.workflowVersion() == null || request.workflowVersion().isBlank()) {
            errors.add("workflowVersion is required");
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Validate inputs against expected schema
     */
    public ValidationResult validateInputs(
        Map<String, Object> inputs,
        Map<String, InputSchema> expectedSchema
    ) {
        List<String> errors = new ArrayList<>();

        // Check required inputs
        for (Map.Entry<String, InputSchema> entry : expectedSchema.entrySet()) {
            String inputName = entry.getKey();
            InputSchema schema = entry.getValue();

            if (schema.required() && !inputs.containsKey(inputName)) {
                errors.add("Required input missing: " + inputName);
            }

            if (inputs.containsKey(inputName)) {
                Object value = inputs.get(inputName);
                if (!validateType(value, schema.type())) {
                    errors.add(String.format(
                        "Input '%s' has wrong type. Expected: %s, Got: %s",
                        inputName, schema.type(), value.getClass().getSimpleName()
                    ));
                }
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    private boolean validateType(Object value, String expectedType) {
        return switch (expectedType.toLowerCase()) {
            case "string" -> value instanceof String;
            case "number", "integer" -> value instanceof Number;
            case "boolean" -> value instanceof Boolean;
            case "object" -> value instanceof Map;
            case "array" -> value instanceof List;
            default -> true; // Unknown types pass validation
        };
    }

    public record ValidationResult(boolean valid, List<String> errors) {
        public void throwIfInvalid() {
            if (!valid) {
                throw new IllegalArgumentException(
                    "Validation failed: " + String.join(", ", errors)
                );
            }
        }
    }

    public record InputSchema(String type, boolean required, Object defaultValue) {}
}
