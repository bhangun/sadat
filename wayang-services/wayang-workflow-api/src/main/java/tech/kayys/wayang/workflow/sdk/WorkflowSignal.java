package tech.kayys.wayang.workflow.sdk;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class WorkflowSignal {
    private String signalType;
    private String source;
    private Map<String, Object> payload;
    private Instant timestamp;
    private String signature;

    public static WorkflowSignal humanApproval(String approver, String comment) {
        return WorkflowSignal.builder()
                .signalType("HUMAN_APPROVAL")
                .source("human")
                .payload(Map.of(
                        "approver", approver,
                        "comment", comment,
                        "approved", true))
                .timestamp(Instant.now())
                .build();
    }

    public static WorkflowSignal externalCallback(String source, Map<String, Object> data) {
        return WorkflowSignal.builder()
                .signalType("EXTERNAL_CALLBACK")
                .source(source)
                .payload(data)
                .timestamp(Instant.now())
                .build();
    }

    public static WorkflowSignal timerExpired(String timerId) {
        return WorkflowSignal.builder()
                .signalType("TIMER_EXPIRED")
                .source("timer")
                .payload(Map.of("timerId", timerId))
                .timestamp(Instant.now())
                .build();
    }
}
