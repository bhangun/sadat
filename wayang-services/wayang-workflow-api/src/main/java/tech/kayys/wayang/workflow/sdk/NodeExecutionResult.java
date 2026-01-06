package tech.kayys.wayang.workflow.sdk;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class NodeExecutionResult {
    private String nodeId;
    private Status status;
    private Instant executedAt;
    private Duration duration;
    private Map<String, Object> outputs;
    private ExecutionError error;
    private Map<String, Object> metadata;

    public enum Status {
        SUCCESS,
        FAILURE,
        WAIT
    }
}