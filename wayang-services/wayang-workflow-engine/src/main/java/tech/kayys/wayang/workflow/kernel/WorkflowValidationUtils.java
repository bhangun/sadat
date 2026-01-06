package tech.kayys.wayang.workflow.kernel;

import tech.kayys.wayang.sdk.util.WorkflowValidator.ValidationResult;
import tech.kayys.wayang.workflow.api.model.RunStatus;
import java.util.Map;

/**
 * Shared validation utilities for workflow state transitions
 */
public final class WorkflowValidationUtils {

    private WorkflowValidationUtils() {
        // Utility class
    }

    public static ValidationResult validateStateTransition(RunStatus from, RunStatus to) {
        if (from == null || to == null) {
            return ValidationResult.failure("From and To states cannot be null");
        }

        if (from == to) {
            return ValidationResult.success(); // No-op transition
        }

        // Check if from state is terminal
        if (isTerminalState(from)) {
            return ValidationResult.failure(
                    String.format("Cannot transition from terminal state %s to %s", from, to));
        }

        // Define valid state transitions
        boolean isValidTransition = switch (from) {
            case PENDING -> to == RunStatus.RUNNING ||
                    to == RunStatus.CANCELLED ||
                    to == RunStatus.TIMED_OUT;
            case RUNNING -> to == RunStatus.SUCCEEDED ||
                    to == RunStatus.FAILED ||
                    to == RunStatus.SUSPENDED ||
                    to == RunStatus.CANCELLED ||
                    to == RunStatus.PAUSED;
            case SUSPENDED -> to == RunStatus.RUNNING ||
                    to == RunStatus.CANCELLED;
            case PAUSED -> to == RunStatus.RUNNING ||
                    to == RunStatus.CANCELLED;
            default -> false;
        };

        if (!isValidTransition) {
            return ValidationResult.failure(
                    String.format("Invalid state transition from %s to %s", from, to));
        }

        return ValidationResult.success();
    }

    public static boolean isTerminalState(RunStatus status) {
        if (status == null) {
            return false;
        }
        return switch (status) {
            case SUCCEEDED, FAILED, CANCELLED, TIMED_OUT -> true;
            default -> false;
        };
    }

    public static ValidationResult validateInputs(Map<String, Object> inputs,
            Map<String, Class<?>> schema) {
        if (schema == null || schema.isEmpty()) {
            return ValidationResult.success(); // No schema validation
        }

        for (Map.Entry<String, Class<?>> entry : schema.entrySet()) {
            String key = entry.getKey();
            Class<?> expectedType = entry.getValue();

            if (!inputs.containsKey(key)) {
                return ValidationResult.failure(
                        String.format("Missing required input: %s", key));
            }

            Object value = inputs.get(key);
            if (value != null && !expectedType.isInstance(value)) {
                return ValidationResult.failure(
                        String.format("Input %s expected type %s but got %s",
                                key, expectedType.getSimpleName(),
                                value.getClass().getSimpleName()));
            }
        }

        return ValidationResult.success();
    }
}