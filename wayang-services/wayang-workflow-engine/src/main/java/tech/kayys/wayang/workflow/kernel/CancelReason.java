package tech.kayys.wayang.workflow.kernel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Reason for cancelling a workflow run.
 * Used for auditing and compensation logic.
 */
@Data
@Builder(toBuilder = true)
public class CancelReason {

    public enum Category {
        USER_REQUEST, // User explicitly cancelled
        SYSTEM_ERROR, // System error requiring cancellation
        TIMEOUT, // Execution timeout
        BUSINESS_RULE, // Business rule violation
        DEPENDENCY_FAILURE, // Dependent service/system failure
        COST_EXCEEDED, // Cost limit exceeded
        SECURITY_VIOLATION, // Security policy violation
        MAINTENANCE, // System maintenance
        OTHER // Other reasons
    }

    private final Category category;
    private final String code;
    private final String description;
    private final String initiatedBy;
    private final Instant initiatedAt;
    private final Map<String, Object> details;
    private final boolean requiresCompensation;

    @JsonCreator
    public CancelReason(
            @JsonProperty("category") Category category,
            @JsonProperty("code") String code,
            @JsonProperty("description") String description,
            @JsonProperty("initiatedBy") String initiatedBy,
            @JsonProperty("initiatedAt") Instant initiatedAt,
            @JsonProperty("details") Map<String, Object> details,
            @JsonProperty("requiresCompensation") boolean requiresCompensation) {

        this.category = category;
        this.code = code;
        this.description = description;
        this.initiatedBy = initiatedBy;
        this.initiatedAt = initiatedAt != null ? initiatedAt : Instant.now();
        this.details = details != null ? Map.copyOf(details) : Map.of();
        this.requiresCompensation = requiresCompensation;
    }

    // Factory methods
    public static CancelReason userRequest(String userId, String reason) {
        return CancelReason.builder()
                .category(Category.USER_REQUEST)
                .code("USER_CANCELLED")
                .description(reason != null ? reason : "Cancelled by user")
                .initiatedBy(userId)
                .requiresCompensation(true)
                .details(Map.of("userId", userId))
                .build();
    }

    public static CancelReason timeout(String timeoutName, Duration duration) {
        return CancelReason.builder()
                .category(Category.TIMEOUT)
                .code("EXECUTION_TIMEOUT")
                .description("Execution timed out after " + duration)
                .initiatedBy("system")
                .requiresCompensation(true)
                .details(Map.of(
                        "timeoutName", timeoutName,
                        "duration", duration.toString(),
                        "threshold", duration.toMillis()))
                .build();
    }

    public static CancelReason systemError(String errorCode, String errorMessage) {
        return CancelReason.builder()
                .category(Category.SYSTEM_ERROR)
                .code(errorCode)
                .description(errorMessage)
                .initiatedBy("system")
                .requiresCompensation(true)
                .details(Map.of("error", errorMessage))
                .build();
    }

    public static CancelReason businessRule(String ruleId, String violation) {
        return CancelReason.builder()
                .category(Category.BUSINESS_RULE)
                .code("BUSINESS_RULE_VIOLATION")
                .description("Business rule violation: " + violation)
                .initiatedBy("system")
                .requiresCompensation(true)
                .details(Map.of("ruleId", ruleId, "violation", violation))
                .build();
    }

    public static CancelReason costExceeded(String limitType, double actual, double limit) {
        return CancelReason.builder()
                .category(Category.COST_EXCEEDED)
                .code("COST_LIMIT_EXCEEDED")
                .description(String.format("Cost exceeded limit: %.2f > %.2f", actual, limit))
                .initiatedBy("system")
                .requiresCompensation(false) // Usually no compensation for cost overruns
                .details(Map.of(
                        "limitType", limitType,
                        "actualCost", actual,
                        "costLimit", limit,
                        "exceededBy", actual - limit))
                .build();
    }

    public boolean isUserInitiated() {
        return category == Category.USER_REQUEST;
    }

    public boolean isSystemInitiated() {
        return !isUserInitiated();
    }
}