package tech.kayys.wayang.workflow.kernel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Describes what triggered a workflow run.
 * Useful for auditing and debugging.
 */
@Data
@Builder(toBuilder = true)
public class RunTrigger {

    public enum Type {
        API, // REST API call
        SCHEDULE, // Cron schedule
        EVENT, // Event-driven (Kafka, etc.)
        MANUAL, // Manual trigger from UI
        WEBHOOK, // External webhook
        INTERNAL, // Internal system trigger
        RETRY, // Automatic retry
        COMPENSATION // Compensation flow
    }

    private final Type type;
    private final String source;
    private final String correlationId;
    private final Map<String, Object> metadata;
    private final Instant triggeredAt;

    @JsonCreator
    public RunTrigger(
            @JsonProperty("type") Type type,
            @JsonProperty("source") String source,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("metadata") Map<String, Object> metadata,
            @JsonProperty("triggeredAt") Instant triggeredAt) {

        this.type = type;
        this.source = source;
        this.correlationId = correlationId;
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
        this.triggeredAt = triggeredAt != null ? triggeredAt : Instant.now();
    }

    // Factory methods
    public static RunTrigger api(String source, String correlationId) {
        return RunTrigger.builder()
                .type(Type.API)
                .source(source)
                .correlationId(correlationId)
                .metadata(Map.of("httpMethod", "POST"))
                .build();
    }

    public static RunTrigger schedule(String scheduleId) {
        return RunTrigger.builder()
                .type(Type.SCHEDULE)
                .source("scheduler")
                .correlationId(scheduleId)
                .metadata(Map.of("scheduleId", scheduleId))
                .build();
    }

    public static RunTrigger event(String eventType, String eventId) {
        return RunTrigger.builder()
                .type(Type.EVENT)
                .source("event-bus")
                .correlationId(eventId)
                .metadata(Map.of("eventType", eventType, "eventId", eventId))
                .build();
    }

    public static RunTrigger manual(String userId) {
        return RunTrigger.builder()
                .type(Type.MANUAL)
                .source("user")
                .correlationId(userId)
                .metadata(Map.of("userId", userId, "userAction", "manual_start"))
                .build();
    }
}
