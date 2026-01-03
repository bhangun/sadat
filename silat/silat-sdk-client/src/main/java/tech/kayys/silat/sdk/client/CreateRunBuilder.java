package tech.kayys.silat.sdk.client;

/**
 * Builder for creating workflow runs
 */
public class CreateRunBuilder {

    private final WorkflowRunClient client;
    private final String workflowDefinitionId;
    private final Map<String, Object> inputs = new HashMap<>();
    private final Map<String, String> labels = new HashMap<>();

    CreateRunBuilder(WorkflowRunClient client, String workflowDefinitionId) {
        this.client = client;
        this.workflowDefinitionId = workflowDefinitionId;
    }

    public CreateRunBuilder input(String key, Object value) {
        inputs.put(key, value);
        return this;
    }

    public CreateRunBuilder inputs(Map<String, Object> inputs) {
        this.inputs.putAll(inputs);
        return this;
    }

    public CreateRunBuilder label(String key, String value) {
        labels.put(key, value);
        return this;
    }

    public CreateRunBuilder labels(Map<String, String> labels) {
        this.labels.putAll(labels);
        return this;
    }

    /**
     * Execute and return the created run
     */
    public Uni<RunResponse> execute() {
        CreateRunRequest request = new CreateRunRequest(
                workflowDefinitionId,
                inputs,
                labels,
                null);
        return client.createRun(request);
    }

    /**
     * Execute and immediately start the run
     */
    public Uni<RunResponse> executeAndStart() {
        return execute()
                .flatMap(run -> client.startRun(run.runId()));
    }
}
