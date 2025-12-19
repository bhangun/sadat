package tech.kayys.wayang.workflow.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Run metadata at checkpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
class RunMetadata {
    private String workflowId;
    private String status;
    private String phase;
    private Integer nodesExecuted;
    private Integer nodesTotal;
    private Long durationMs;
    private Integer priority;
    private Instant slaDeadline;
}