package tech.kayys.silat.sdk.client;

/**
 * Builder for querying runs
 */
public class QueryRunsBuilder {

    private final WorkflowRunClient client;
    private String workflowId;
    private String status;
    private int page = 0;
    private int size = 20;

    QueryRunsBuilder(WorkflowRunClient client) {
        this.client = client;
    }

    public QueryRunsBuilder workflowId(String workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    public QueryRunsBuilder status(String status) {
        this.status = status;
        return this;
    }

    public QueryRunsBuilder page(int page) {
        this.page = page;
        return this;
    }

    public QueryRunsBuilder size(int size) {
        this.size = size;
        return this;
    }

    public Uni<PagedResponse<RunResponse>> execute() {
        return client.queryRuns(workflowId, status, page, size);
    }
}
