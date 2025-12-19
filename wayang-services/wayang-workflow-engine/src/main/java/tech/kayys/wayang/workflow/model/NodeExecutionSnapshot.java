package tech.kayys.wayang.workflow.model;

import java.time.Instant;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Node execution snapshot
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class NodeExecutionSnapshot {
    private String nodeId;
    private String nodeType;
    private String status; // SUCCESS, ERROR, SKIPPED, RUNNING
    private Instant startedAt;
    private Instant completedAt;
    private Integer attemptNumber;
    private Map<String, Object> input;
    private Map<String, Object> output;
    private ErrorSnapshot error;
    private Map<String, Object> metadata;
}
