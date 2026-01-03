package tech.kayys.silat.sdk.client;

/**
 * REST-based workflow run client
 */
public class RestWorkflowRunClient implements WorkflowRunClient {

    private final SilatClientConfig config;
    private final io.vertx.mutiny.core.Vertx vertx;
    private final io.vertx.mutiny.ext.web.client.WebClient webClient;

    RestWorkflowRunClient(SilatClientConfig config) {
        this.config = config;
        this.vertx = io.vertx.mutiny.core.Vertx.vertx();
        this.webClient = io.vertx.mutiny.ext.web.client.WebClient.create(vertx);
    }

    @Override
    public Uni<RunResponse> createRun(CreateRunRequest request) {
        return webClient.post(config.endpoint() + "/api/v1/runs")
                .putHeader("X-Tenant-ID", config.tenantId())
                .putHeader("Authorization", "Bearer " + config.apiKey())
                .sendJson(request)
                .map(response -> response.bodyAsJson(RunResponse.class));
    }

    // Implement other methods similarly...

    @Override
    public Uni<RunResponse> getRun(String runId) {
        return webClient.get(config.endpoint() + "/api/v1/runs/" + runId)
                .putHeader("X-Tenant-ID", config.tenantId())
                .putHeader("Authorization", "Bearer " + config.apiKey())
                .send()
                .map(response -> response.bodyAsJson(RunResponse.class));
    }

    // ... (other methods)

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
