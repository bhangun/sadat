package tech.kayys.wayang.workflow.kernel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Health status of an executor.
 * Used for load balancing and failover.
 */
@Data
@Builder(toBuilder = true)
public class ExecutorHealth {

    public enum Status {
        HEALTHY, // Fully operational
        DEGRADED, // Operational but with issues
        UNHEALTHY, // Not operational
        UNKNOWN // Status unknown
    }

    private final String executorId;
    private final Status status;
    private final Instant checkedAt;
    private final String message;
    private final Map<String, Object> metrics;
    private final Map<String, Object> details;

    @JsonCreator
    public ExecutorHealth(
            @JsonProperty("executorId") String executorId,
            @JsonProperty("status") Status status,
            @JsonProperty("checkedAt") Instant checkedAt,
            @JsonProperty("message") String message,
            @JsonProperty("metrics") Map<String, Object> metrics,
            @JsonProperty("details") Map<String, Object> details) {

        this.executorId = executorId;
        this.status = status;
        this.checkedAt = checkedAt != null ? checkedAt : Instant.now();
        this.message = message;
        this.metrics = metrics != null ? Map.copyOf(metrics) : Map.of();
        this.details = details != null ? Map.copyOf(details) : Map.of();
    }

    // Factory methods
    public static ExecutorHealth healthy(String executorId) {
        return ExecutorHealth.builder()
                .executorId(executorId)
                .status(Status.HEALTHY)
                .message("Executor is healthy")
                .metrics(Map.of("responseTimeMs", 50, "successRate", 0.99))
                .build();
    }

    public static ExecutorHealth healthy(String executorId, Map<String, Object> metrics) {
        return ExecutorHealth.builder()
                .executorId(executorId)
                .status(Status.HEALTHY)
                .message("Executor is healthy")
                .metrics(metrics)
                .build();
    }

    public static ExecutorHealth degraded(String executorId, String reason, Map<String, Object> details) {
        return ExecutorHealth.builder()
                .executorId(executorId)
                .status(Status.DEGRADED)
                .message("Executor is degraded: " + reason)
                .details(details != null ? details : Map.of())
                .build();
    }

    public static ExecutorHealth unhealthy(String executorId, String error) {
        return ExecutorHealth.builder()
                .executorId(executorId)
                .status(Status.UNHEALTHY)
                .message("Executor is unhealthy: " + error)
                .details(Map.of("error", error))
                .build();
    }

    public static ExecutorHealth unknown(String executorId) {
        return ExecutorHealth.builder()
                .executorId(executorId)
                .status(Status.UNKNOWN)
                .message("Executor status unknown")
                .build();
    }

    public boolean isHealthy() {
        return status == Status.HEALTHY;
    }

    public boolean isOperational() {
        return status == Status.HEALTHY || status == Status.DEGRADED;
    }

    public double getSuccessRate() {
        Object rate = metrics.get("successRate");
        if (rate instanceof Number) {
            return ((Number) rate).doubleValue();
        }
        return status == Status.HEALTHY ? 1.0 : 0.0;
    }

    public long getResponseTime() {
        Object time = metrics.get("responseTimeMs");
        if (time instanceof Number) {
            return ((Number) time).longValue();
        }
        return 0L;
    }

    public int getCurrentLoad() {
        Object load = metrics.get("currentLoad");
        if (load instanceof Number) {
            return ((Number) load).intValue();
        }
        return 0;
    }
}
