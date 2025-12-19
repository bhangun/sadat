package tech.kayys.wayang.workflow.model;

import java.time.Instant;
import java.util.Map;

import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.api.model.WorkflowEventType;
import tech.kayys.wayang.sdk.dto.NodeExecutionState;
import tech.kayys.wayang.workflow.api.model.RunStatus;

/**
 * Workflow Event (immutable)
 */
public record WorkflowEvent(
                String id,
                String runId,
                Long sequence,
                WorkflowEventType type,
                Map<String, Object> data,
                Instant timestamp) {
        // Factory methods
        public static WorkflowEvent created(WorkflowRun run, Map<String, Object> inputs) {
                return new WorkflowEvent(
                                null, run.getRunId(), null,
                                WorkflowEventType.CREATED,
                                Map.of(
                                                "workflowId", run.getWorkflowId(),
                                                "tenantId", run.getTenantId(),
                                                "triggeredBy", run.getTriggeredBy(),
                                                "inputs", inputs),
                                null);
        }

        public static WorkflowEvent statusChanged(
                        String runId,
                        RunStatus oldStatus,
                        RunStatus newStatus) {
                return new WorkflowEvent(
                                null, runId, null,
                                WorkflowEventType.STATUS_CHANGED,
                                Map.of(
                                                "oldStatus", oldStatus.toString(),
                                                "newStatus", newStatus.toString()),
                                null);
        }

        public static WorkflowEvent nodeExecuted(String runId, NodeExecutionState nodeState) {
                return new WorkflowEvent(
                                null, runId, null,
                                WorkflowEventType.NODE_EXECUTED,
                                Map.of(
                                                "nodeId", nodeState.nodeId(),
                                                "status", nodeState.status().toString(),
                                                "inputs", nodeState.inputs() != null ? nodeState.inputs() : Map.of(),
                                                "outputs", nodeState.outputs() != null ? nodeState.outputs() : Map.of(),
                                                "errorMessage",
                                                nodeState.errorMessage() != null ? nodeState.errorMessage() : "",
                                                "startedAt",
                                                nodeState.startedAt() != null ? nodeState.startedAt().toString() : "",
                                                "completedAt",
                                                nodeState.completedAt() != null ? nodeState.completedAt().toString()
                                                                : ""),
                                null);
        }

        public static WorkflowEvent stateUpdated(String runId, Map<String, Object> updates) {
                return new WorkflowEvent(
                                null, runId, null,
                                WorkflowEventType.STATE_UPDATED,
                                Map.of("stateUpdates", updates),
                                null);
        }

        public static WorkflowEvent resumed(
                        String runId,
                        String correlationKey,
                        Map<String, Object> resumeData,
                        String resumedBy) {
                return new WorkflowEvent(
                                null, runId, null,
                                WorkflowEventType.RESUMED,
                                Map.of(
                                                "correlationKey", correlationKey,
                                                "resumeData", resumeData,
                                                "resumedBy", resumedBy),
                                null);
        }

        public static WorkflowEvent cancelled(String runId, String reason, String cancelledBy) {
                return new WorkflowEvent(
                                null, runId, null,
                                WorkflowEventType.CANCELLED,
                                Map.of(
                                                "reason", reason,
                                                "cancelledBy", cancelledBy),
                                null);
        }

        public static WorkflowEvent completed(String runId, Map<String, Object> outputs) {
                return new WorkflowEvent(
                                null, runId, null,
                                WorkflowEventType.STATUS_CHANGED,
                                Map.of(
                                                "newStatus", RunStatus.SUCCEEDED.toString(),
                                                "outputs", outputs != null ? outputs : Map.of()),
                                null);
        }
}