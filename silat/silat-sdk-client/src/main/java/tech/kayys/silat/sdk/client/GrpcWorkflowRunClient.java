package tech.kayys.silat.sdk.client;

import java.util.Map;

import io.smallrye.mutiny.Uni;

/**
 * gRPC-based workflow run client
 */
class GrpcWorkflowRunClient implements WorkflowRunClient {

    private final SilatClientConfig config;
    // gRPC stub would be injected here

    GrpcWorkflowRunClient(SilatClientConfig config) {
        this.config = config;
    }

    // Implement using gRPC stubs...

    @Override
    public Uni<RunResponse> createRun(CreateRunRequest request) {
        return null;
    }

    @Override
    public Uni<RunResponse> getRun(String runId) {
        return null;
    }

    @Override
    public Uni<RunResponse> startRun(String runId) {
        return null;
    }

    @Override
    public Uni<RunResponse> suspendRun(String runId, String reason, String waitingOnNodeId) {
        return null;
    }

    @Override
    public Uni<RunResponse> resumeRun(String runId, Map<String, Object> resumeData, String humanTaskId) {
        return null;
    }

    @Override
    public Uni<Void> cancelRun(String runId, String reason) {
        return null;
    }

    @Override
    public Uni<Void> signal(String runId, String signalName, String targetNodeId, Map<String, Object> payload) {
        return null;
    }

    @Override
    public Uni<ExecutionHistoryResponse> getExecutionHistory(String runId) {
        return null;
    }

    @Override
    public Uni<PagedResponse<RunResponse>> queryRuns(String workflowId, String status, int page, int size) {
        return null;
    }

    @Override
    public Uni<Long> getActiveRunsCount() {
        return null;
    }
}
