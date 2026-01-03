package tech.kayys.silat.execution;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

/**
 * Record of a node execution for history tracking.
 */
@Data
@Builder
public class NodeExecutionRecord {
    private final String nodeId;
    private final NodeExecutionStatus status;
    private final Instant startedAt;
    private final Instant completedAt;
    private final Duration duration;
    private final Map<String, Object> inputs;
    private final Map<String, Object> outputs;
    private final ExecutionError error;
    private final Map<String, Object> metadata;
    private final int attempt;
}
