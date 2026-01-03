package tech.kayys.silat.sdk.client;

import java.util.Map;

import io.smallrye.mutiny.Uni;

/**
 * Workflow run client interface (transport-agnostic)
 */
interface WorkflowRunClient {
    Uni<RunResponse> createRun(CreateRunRequest request);

    Uni<RunResponse> getRun(String runId);

    Uni<RunResponse> startRun(String runId);

    Uni<RunResponse> suspendRun(String runId, String reason, String waitingOnNodeId);

    Uni<RunResponse> resumeRun(String runId, Map<String, Object> resumeData, String humanTaskId);

    Uni<Void> cancelRun(String runId, String reason);

    Uni<Void> signal(String runId, String signalName, String targetNodeId, Map<String, Object> payload);

    Uni<ExecutionHistoryResponse> getExecutionHistory(String runId);

    Uni<PagedResponse<RunResponse>> queryRuns(String workflowId, String status, int page, int size);

    Uni<Long> getActiveRunsCount();
}