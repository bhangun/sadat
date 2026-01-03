package tech.kayys.silat.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Information about why a workflow is waiting.
 * Used for human-in-the-loop, external callbacks, timers, etc.
 */
@Data
@Builder(toBuilder = true)
public class WaitInfo {

    public enum WaitType {
        HUMAN_APPROVAL, // Waiting for human approval
        EXTERNAL_CALLBACK, // Waiting for external service callback
        TIMER, // Waiting for timer expiration
        CONDITION, // Waiting for condition to be met
        RESOURCE, // Waiting for resource availability
        DEPENDENCY, // Waiting for dependency completion
        MANUAL_INTERVENTION, // Waiting for manual intervention
        RATE_LIMIT, // Waiting due to rate limiting
        CUSTOM // Custom wait type
    }

    private final String waitId;
    private final WaitType waitType;
    private final String nodeId;
    private final String reason;
    private final Instant waitStartedAt;
    private final Duration timeout;
    private final Map<String, Object> waitData;
    private final Map<String, Object> metadata;

    @JsonCreator
    public WaitInfo(
            @JsonProperty("waitId") String waitId,
            @JsonProperty("waitType") WaitType waitType,
            @JsonProperty("nodeId") String nodeId,
            @JsonProperty("reason") String reason,
            @JsonProperty("waitStartedAt") Instant waitStartedAt,
            @JsonProperty("timeout") Duration timeout,
            @JsonProperty("waitData") Map<String, Object> waitData,
            @JsonProperty("metadata") Map<String, Object> metadata) {

        this.waitId = waitId != null ? waitId : java.util.UUID.randomUUID().toString();
        this.waitType = waitType;
        this.nodeId = nodeId;
        this.reason = reason;
        this.waitStartedAt = waitStartedAt != null ? waitStartedAt : Instant.now();
        this.timeout = timeout;
        this.waitData = waitData != null ? Map.copyOf(waitData) : Map.of();
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }

    // Factory methods
    public static WaitInfo humanApproval(String nodeId, String approvalRequestId, String approver) {
        return WaitInfo.builder()
                .waitType(WaitType.HUMAN_APPROVAL)
                .nodeId(nodeId)
                .reason("Waiting for human approval")
                .timeout(Duration.ofDays(7))
                .waitData(Map.of(
                        "approvalRequestId", approvalRequestId,
                        "approver", approver,
                        "requiredAction", "approve_or_reject"))
                .metadata(Map.of("requiresHuman", true, "interactive", true))
                .build();
    }

    public static WaitInfo externalCallback(String nodeId, String callbackUrl, String expectedResponse) {
        return WaitInfo.builder()
                .waitType(WaitType.EXTERNAL_CALLBACK)
                .nodeId(nodeId)
                .reason("Waiting for external service callback")
                .timeout(Duration.ofHours(1))
                .waitData(Map.of(
                        "callbackUrl", callbackUrl,
                        "expectedResponse", expectedResponse,
                        "retryAttempt", 0))
                .metadata(Map.of("externalService", true, "async", true))
                .build();
    }

    public static WaitInfo timer(String nodeId, Duration duration, String timerName) {
        return WaitInfo.builder()
                .waitType(WaitType.TIMER)
                .nodeId(nodeId)
                .reason("Waiting for timer: " + timerName)
                .timeout(duration.plus(Duration.ofMinutes(5))) // Extra buffer
                .waitData(Map.of(
                        "duration", duration.toMillis(),
                        "timerName", timerName,
                        "expiresAt", Instant.now().plus(duration).toString()))
                .metadata(Map.of("scheduled", true, "autoResume", true))
                .build();
    }

    public static WaitInfo condition(String nodeId, String conditionExpression) {
        return WaitInfo.builder()
                .waitType(WaitType.CONDITION)
                .nodeId(nodeId)
                .reason("Waiting for condition: " + conditionExpression)
                .timeout(Duration.ofHours(24))
                .waitData(Map.of(
                        "condition", conditionExpression,
                        "pollInterval", Duration.ofSeconds(30).toMillis()))
                .metadata(Map.of("conditional", true, "pollingRequired", true))
                .build();
    }

    public boolean isExpired() {
        if (timeout == null) {
            return false;
        }
        Instant expiresAt = waitStartedAt.plus(timeout);
        return Instant.now().isAfter(expiresAt);
    }

    public Duration getRemainingTime() {
        if (timeout == null) {
            return Duration.ZERO;
        }
        Instant expiresAt = waitStartedAt.plus(timeout);
        return Duration.between(Instant.now(), expiresAt);
    }

    public boolean isHumanInvolved() {
        return waitType == WaitType.HUMAN_APPROVAL ||
                waitType == WaitType.MANUAL_INTERVENTION;
    }

    public boolean isExternalService() {
        return waitType == WaitType.EXTERNAL_CALLBACK;
    }

    public boolean isTimer() {
        return waitType == WaitType.TIMER;
    }
}