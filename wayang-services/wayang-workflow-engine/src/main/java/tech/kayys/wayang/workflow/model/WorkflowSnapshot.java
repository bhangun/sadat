package tech.kayys.wayang.workflow.model;

import java.time.Instant;

import tech.kayys.wayang.workflow.domain.WorkflowRun;

/**
 * Workflow snapshot for performance
 */
public record WorkflowSnapshot(
                String runId,
                WorkflowRun state,
                long eventCount,
                Instant createdAt) {
}