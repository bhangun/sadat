package tech.kayys.silat.executor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Error information from an executor.
 * Separate from ExecutionError which is about node execution.
 */
@Data
@Builder(toBuilder = true)
public class ExecutorError {

        public enum Category {
                CONNECTION, // Connection issues
                TIMEOUT, // Timeout errors
                RESOURCE, // Resource constraints
                CONFIGURATION, // Configuration errors
                VALIDATION, // Validation errors
                INTERNAL, // Internal executor errors
                EXTERNAL, // External service errors
                SECURITY, // Security violations
                UNKNOWN // Unknown errors
        }

        private final String errorId;
        private final String executorId;
        private final Category category;
        private final String code;
        private final String message;
        private final Instant occurredAt;
        private final Map<String, Object> details;
        private final boolean retriable;
        private final String recoveryHint;

        @JsonCreator
        public ExecutorError(
                        @JsonProperty("errorId") String errorId,
                        @JsonProperty("executorId") String executorId,
                        @JsonProperty("category") Category category,
                        @JsonProperty("code") String code,
                        @JsonProperty("message") String message,
                        @JsonProperty("occurredAt") Instant occurredAt,
                        @JsonProperty("details") Map<String, Object> details,
                        @JsonProperty("retriable") boolean retriable,
                        @JsonProperty("recoveryHint") String recoveryHint) {

                this.errorId = errorId != null ? errorId : java.util.UUID.randomUUID().toString();
                this.executorId = executorId;
                this.category = category;
                this.code = code;
                this.message = message;
                this.occurredAt = occurredAt != null ? occurredAt : Instant.now();
                this.details = details != null ? Map.copyOf(details) : Map.of();
                this.retriable = retriable;
                this.recoveryHint = recoveryHint;
        }

        // Factory methods
        public static ExecutorError connectionError(String executorId, String endpoint, Throwable cause) {
                return ExecutorError.builder()
                                .executorId(executorId)
                                .category(Category.CONNECTION)
                                .code("EXECUTOR_CONNECTION_FAILED")
                                .message("Failed to connect to executor at " + endpoint + ": " + cause.getMessage())
                                .retriable(true)
                                .recoveryHint("Check network connectivity and executor status")
                                .details(Map.of(
                                                "endpoint", endpoint,
                                                "errorType", cause.getClass().getName(),
                                                "errorMessage", cause.getMessage()))
                                .build();
        }

        public static ExecutorError timeoutError(String executorId, Duration timeout, String operation) {
                return ExecutorError.builder()
                                .executorId(executorId)
                                .category(Category.TIMEOUT)
                                .code("EXECUTOR_TIMEOUT")
                                .message("Executor timed out after " + timeout + " during " + operation)
                                .retriable(true)
                                .recoveryHint("Increase timeout or optimize executor performance")
                                .details(Map.of(
                                                "timeoutMs", timeout.toMillis(),
                                                "operation", operation,
                                                "suggestedTimeout", timeout.plus(Duration.ofSeconds(30)).toMillis()))
                                .build();
        }

        public static ExecutorError resourceError(String executorId, String resource, String constraint) {
                return ExecutorError.builder()
                                .executorId(executorId)
                                .category(Category.RESOURCE)
                                .code("EXECUTOR_RESOURCE_LIMIT")
                                .message("Executor resource limit exceeded: " + resource + " (" + constraint + ")")
                                .retriable(true)
                                .recoveryHint("Scale executor resources or reduce load")
                                .details(Map.of(
                                                "resource", resource,
                                                "constraint", constraint,
                                                "suggestedAction", "scale_up_or_throttle"))
                                .build();
        }

        public static ExecutorError configurationError(String executorId, String configKey, String issue) {
                return ExecutorError.builder()
                                .executorId(executorId)
                                .category(Category.CONFIGURATION)
                                .code("EXECUTOR_CONFIG_ERROR")
                                .message("Executor configuration error: " + configKey + " - " + issue)
                                .retriable(false) // Usually not retriable without config change
                                .recoveryHint("Fix executor configuration and restart")
                                .details(Map.of(
                                                "configKey", configKey,
                                                "issue", issue,
                                                "requiresRestart", true))
                                .build();
        }

        public static ExecutorError validationError(String executorId, String validationRule, String details) {
                return ExecutorError.builder()
                                .executorId(executorId)
                                .category(Category.VALIDATION)
                                .code("EXECUTOR_VALIDATION_FAILED")
                                .message("Executor validation failed: " + validationRule)
                                .retriable(false)
                                .recoveryHint("Fix input data or adjust validation rules")
                                .details(Map.of(
                                                "validationRule", validationRule,
                                                "details", details,
                                                "requiresInputFix", true))
                                .build();
        }

        public static ExecutorError internalError(String executorId, String component, Throwable cause) {
                return ExecutorError.builder()
                                .executorId(executorId)
                                .category(Category.INTERNAL)
                                .code("EXECUTOR_INTERNAL_ERROR")
                                .message("Executor internal error in " + component + ": " + cause.getMessage())
                                .retriable(cause.getMessage().contains("temporary") ||
                                                cause.getMessage().contains("retry"))
                                .recoveryHint("Check executor logs and restart if necessary")
                                .details(Map.of(
                                                "component", component,
                                                "errorType", cause.getClass().getName(),
                                                "stackTrace", getStackTrace(cause)))
                                .build();
        }

        private static String getStackTrace(Throwable t) {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                t.printStackTrace(pw);
                return sw.toString();
        }

        public boolean isConnectionError() {
                return category == Category.CONNECTION;
        }

        public boolean isTimeoutError() {
                return category == Category.TIMEOUT;
        }

        public boolean isRecoverable() {
                return retriable ||
                                category == Category.CONNECTION ||
                                category == Category.TIMEOUT ||
                                category == Category.RESOURCE;
        }
}
