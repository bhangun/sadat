package tech.kayys.wayang.workflow.model;

import java.time.Instant;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;
import tech.kayys.wayang.schema.execution.ErrorPayload;

/**
 * Node execution record
 */
@Data
@NoArgsConstructor
class NodeExecutionRecord {
    private String nodeId;
    private String nodeType;
    private Instant startedAt;
    private Instant completedAt;
    private String status; // SUCCESS, ERROR, SKIPPED
    private Map<String, Object> input;
    private Map<String, Object> output;
    private ErrorPayload error;
    private Integer attemptNumber = 1;
}
