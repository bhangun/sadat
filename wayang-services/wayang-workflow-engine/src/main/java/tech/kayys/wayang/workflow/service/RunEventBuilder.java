package tech.kayys.wayang.workflow.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import tech.kayys.wayang.schema.execution.ErrorPayload;
import tech.kayys.wayang.workflow.domain.Checkpoint;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.model.RunEvent;

/**
 * RunEventBuilder - Factory for workflow run events.
 * 
 * Purpose:
 * - Standardize event structure
 * - Enable event-driven architecture
 * - Support observability and monitoring
 * - Enable integration with external systems
 * 
 * Event Types:
 * - Lifecycle: created, started, completed, failed, cancelled
 * - State: suspended, resumed, restored
 * - Recovery: retry_scheduled, retry_attempted
 * - Monitoring: heartbeat, timeout
 */
public class RunEventBuilder {

        /**
         * Run created event
         */
        public static RunEvent created(WorkflowRun run) {
                return RunEvent.builder()
                                .eventType("workflow.run.created")
                                .runId(run.getRunId())
                                .workflowId(run.getWorkflowId())
                                .tenantId(run.getTenantId())
                                .status(run.getStatus().name())
                                .timestamp(run.getCreatedAt())
                                .metadata(Map.of(
                                                "triggeredBy", run.getTriggeredBy(),
                                                "triggerType", run.getTriggerType(),
                                                "correlationId",
                                                run.getCorrelationId() != null ? run.getCorrelationId() : ""))
                                .build();
        }

        /**
         * Run started event
         */
        public static RunEvent started(WorkflowRun run) {
                return RunEvent.builder()
                                .eventType("workflow.run.started")
                                .runId(run.getRunId())
                                .workflowId(run.getWorkflowId())
                                .tenantId(run.getTenantId())
                                .status(run.getStatus().name())
                                .timestamp(run.getStartedAt())
                                .metadata(Map.of(
                                                "nodesTotal", run.getNodesTotal(),
                                                "attemptNumber", run.getAttemptNumber()))
                                .build();
        }

        /**
         * Run suspended event
         */
        public static RunEvent suspended(WorkflowRun run, String reason) {
                return RunEvent.builder()
                                .eventType("workflow.run.suspended")
                                .runId(run.getRunId())
                                .workflowId(run.getWorkflowId())
                                .tenantId(run.getTenantId())
                                .status(run.getStatus().name())
                                .timestamp(Instant.now())
                                .metadata(Map.of(
                                                "reason", reason,
                                                "currentNode",
                                                run.getExecutionState().getCurrentNodeId() != null
                                                                ? run.getExecutionState().getCurrentNodeId()
                                                                : "",
                                                "pendingHumanTasks",
                                                run.getExecutionState().getPendingHumanTasks().size()))
                                .build();
        }

        /**
         * Run resumed event
         */
        public static RunEvent resumed(WorkflowRun run) {
                return RunEvent.builder()
                                .eventType("workflow.run.resumed")
                                .runId(run.getRunId())
                                .workflowId(run.getWorkflowId())
                                .tenantId(run.getTenantId())
                                .status(run.getStatus().name())
                                .timestamp(Instant.now())
                                .metadata(Map.of(
                                                "currentNode",
                                                run.getExecutionState().getCurrentNodeId() != null
                                                                ? run.getExecutionState().getCurrentNodeId()
                                                                : ""))
                                .build();
        }

        /**
         * Run completed event
         */
        public static RunEvent completed(WorkflowRun run) {
                return RunEvent.builder()
                                .eventType("workflow.run.completed")
                                .runId(run.getRunId())
                                .workflowId(run.getWorkflowId())
                                .tenantId(run.getTenantId())
                                .status(run.getStatus().name())
                                .timestamp(run.getCompletedAt())
                                .metadata(Map.of(
                                                "durationMs", run.getDurationMs() != null ? run.getDurationMs() : 0,
                                                "nodesExecuted", run.getNodesExecuted(),
                                                "attemptNumber", run.getAttemptNumber()))
                                .build();
        }

        /**
         * Run failed event
         */
        public static RunEvent failed(WorkflowRun run, ErrorPayload error) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("errorType", error.getType());
                metadata.put("errorMessage", error.getMessage());
                metadata.put("retryable", error.isRetryable());
                metadata.put("attemptNumber", run.getAttemptNumber());
                metadata.put("maxAttempts", run.getMaxAttempts());

                if (error.getOriginNode() != null) {
                        metadata.put("originNode", error.getOriginNode());
                }

                return RunEvent.builder()
                                .eventType("workflow.run.failed")
                                .runId(run.getRunId())
                                .workflowId(run.getWorkflowId())
                                .tenantId(run.getTenantId())
                                .status(run.getStatus().name())
                                .timestamp(run.getCompletedAt())
                                .metadata(metadata)
                                .build();
        }

        /**
         * Run cancelled event
         */
        public static RunEvent cancelled(WorkflowRun run, String reason) {
                return RunEvent.builder()
                                .eventType("workflow.run.cancelled")
                                .runId(run.getRunId())
                                .workflowId(run.getWorkflowId())
                                .tenantId(run.getTenantId())
                                .status(run.getStatus().name())
                                .timestamp(run.getCompletedAt())
                                .metadata(Map.of(
                                                "reason", reason,
                                                "nodesExecuted", run.getNodesExecuted()))
                                .build();
        }

        /**
         * Run timeout event
         */
        public static RunEvent timeout(WorkflowRun run) {
                return RunEvent.builder()
                                .eventType("workflow.run.timeout")
                                .runId(run.getRunId())
                                .workflowId(run.getWorkflowId())
                                .tenantId(run.getTenantId())
                                .status(run.getStatus().name())
                                .timestamp(run.getCompletedAt())
                                .metadata(Map.of(
                                                "durationMs", run.getDurationMs() != null ? run.getDurationMs() : 0,
                                                "lastHeartbeat",
                                                run.getLastHeartbeatAt() != null ? run.getLastHeartbeatAt().toString()
                                                                : "never"))
                                .build();
        }

        /**
         * Retry scheduled event
         */
        public static RunEvent retryScheduled(WorkflowRun run, long delayMs) {
                return RunEvent.builder()
                                .eventType("workflow.run.retry_scheduled")
                                .runId(run.getRunId())
                                .workflowId(run.getWorkflowId())
                                .tenantId(run.getTenantId())
                                .status(run.getStatus().name())
                                .timestamp(Instant.now())
                                .metadata(Map.of(
                                                "attemptNumber", run.getAttemptNumber(),
                                                "maxAttempts", run.getMaxAttempts(),
                                                "delayMs", delayMs,
                                                "nextRetryAt", run.getNextRetryAt().toString()))
                                .build();
        }

        /**
         * Retry attempted event
         */
        public static RunEvent retryAttempted(WorkflowRun run) {
                return RunEvent.builder()
                                .eventType("workflow.run.retry_attempted")
                                .runId(run.getRunId())
                                .workflowId(run.getWorkflowId())
                                .tenantId(run.getTenantId())
                                .status(run.getStatus().name())
                                .timestamp(Instant.now())
                                .metadata(Map.of(
                                                "attemptNumber", run.getAttemptNumber(),
                                                "maxAttempts", run.getMaxAttempts()))
                                .build();
        }

        /**
         * Run restored event (from checkpoint)
         */
        public static RunEvent restored(WorkflowRun run, Checkpoint checkpoint) {
                return RunEvent.builder()
                                .eventType("workflow.run.restored")
                                .runId(run.getRunId())
                                .workflowId(run.getWorkflowId())
                                .tenantId(run.getTenantId())
                                .status(run.getStatus().name())
                                .timestamp(Instant.now())
                                .metadata(Map.of(
                                                "checkpointId", checkpoint.getCheckpointId(),
                                                "checkpointSequence", checkpoint.getSequenceNumber(),
                                                "nodesExecuted", checkpoint.getNodesExecuted()))
                                .build();
        }

        /**
         * Progress updated event (for monitoring)
         */
        public static RunEvent progress(
                        WorkflowRun run,
                        String currentNode,
                        int nodesCompleted,
                        int nodesTotal) {

                return RunEvent.builder()
                                .eventType("workflow.run.progress")
                                .runId(run.getRunId())
                                .workflowId(run.getWorkflowId())
                                .tenantId(run.getTenantId())
                                .status(run.getStatus().name())
                                .timestamp(Instant.now())
                                .metadata(Map.of(
                                                "currentNode", currentNode,
                                                "nodesCompleted", nodesCompleted,
                                                "nodesTotal", nodesTotal,
                                                "percentComplete",
                                                nodesTotal > 0 ? (nodesCompleted * 100 / nodesTotal) : 0))
                                .build();
        }
}
