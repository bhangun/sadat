package tech.kayys.wayang.workflow.kernel;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Signal to resume a waiting workflow run.
 * Used for human approvals, external callbacks, timer expirations, etc.
 */
@Data
@Builder(toBuilder = true)
public class Signal {

    public enum Type {
        HUMAN_APPROVAL, // Human approval/rejection
        EXTERNAL_CALLBACK, // External service callback
        TIMER_EXPIRED, // Timer expiration
        MANUAL_INTERVENTION, // Manual override
        DATA_UPDATE, // Data update from external source
        CONTINUE, // Simple continue signal
        CANCEL, // Cancel request
        ESCALATE, // Escalation signal
        CUSTOM // Custom signal type
    }

    private final String signalId;
    private final Type type;
    private final String source;
    private final String correlationId;
    private final Map<String, Object> payload;
    private final Instant timestamp;
    private final String signature;

    @JsonCreator
    public Signal(
            @JsonProperty("signalId") String signalId,
            @JsonProperty("type") Type type,
            @JsonProperty("source") String source,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("payload") Map<String, Object> payload,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("signature") String signature) {

        this.signalId = signalId != null ? signalId : UUID.randomUUID().toString();
        this.type = type;
        this.source = source;
        this.correlationId = correlationId;
        this.payload = payload != null ? Map.copyOf(payload) : Map.of();
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.signature = signature;
    }

    // Factory methods
    public static Signal humanApproval(String approver, boolean approved, String comment) {
        return Signal.builder()
                .type(Type.HUMAN_APPROVAL)
                .source("human")
                .payload(Map.of(
                        "approver", approver,
                        "approved", approved,
                        "comment", comment != null ? comment : "",
                        "timestamp", Instant.now().toString()))
                .build();
    }

    public static Signal externalCallback(String source, String callbackId, Map<String, Object> data) {
        return Signal.builder()
                .type(Type.EXTERNAL_CALLBACK)
                .source(source)
                .correlationId(callbackId)
                .payload(data != null ? data : Map.of())
                .build();
    }

    public static Signal timerExpired(String timerId) {
        return Signal.builder()
                .type(Type.TIMER_EXPIRED)
                .source("timer-service")
                .correlationId(timerId)
                .payload(Map.of("timerId", timerId))
                .build();
    }

    public static Signal continueSignal(String source) {
        return Signal.builder()
                .type(Type.CONTINUE)
                .source(source)
                .payload(Map.of("action", "continue"))
                .build();
    }

    public static Signal dataUpdate(String source, Map<String, Object> updates) {
        return Signal.builder()
                .type(Type.DATA_UPDATE)
                .source(source)
                .payload(Map.of("updates", updates))
                .build();
    }

    public boolean isValid() {
        if (signalId == null || signalId.isEmpty())
            return false;
        if (type == null)
            return false;
        if (source == null || source.isEmpty())
            return false;
        if (timestamp == null)
            return false;
        return true;
    }

    public <T> T getPayloadValue(String key, Class<T> type) {
        Object value = payload.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }
}
