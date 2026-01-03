package tech.kayys.silat.execution;

import java.time.Instant;
import java.util.Map;

import tech.kayys.silat.model.ErrorSnapshot;

/**
 * Node Execution Snapshot - Nested in WorkflowRunEntity
 */
public record NodeExecutionSnapshot(
                String nodeId,
                String status,
                int attempt,
                Instant startedAt,
                Instant completedAt,
                Map<String, Object> output,
                ErrorSnapshot error) {
}
