package tech.kayys.silat.sdk.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.smallrye.mutiny.Uni;

/**
 * Builder for workflow definitions
 */
public class WorkflowDefinitionBuilder {

    private final WorkflowDefinitionClient client;
    private final String name;
    private String version = "1.0.0";
    private String description;
    private final List<NodeDefinitionDto> nodes = new ArrayList<>();
    private final Map<String, InputDefinitionDto> inputs = new HashMap<>();
    private final Map<String, OutputDefinitionDto> outputs = new HashMap<>();
    private RetryPolicyDto retryPolicy;
    private CompensationPolicyDto compensationPolicy;
    private final Map<String, String> metadata = new HashMap<>();

    WorkflowDefinitionBuilder(WorkflowDefinitionClient client, String name) {
        this.client = client;
        this.name = name;
    }

    public WorkflowDefinitionBuilder version(String version) {
        this.version = version;
        return this;
    }

    public WorkflowDefinitionBuilder description(String description) {
        this.description = description;
        return this;
    }

    public WorkflowDefinitionBuilder addNode(NodeDefinitionDto node) {
        nodes.add(node);
        return this;
    }

    public WorkflowDefinitionBuilder addInput(String name, InputDefinitionDto input) {
        inputs.put(name, input);
        return this;
    }

    public WorkflowDefinitionBuilder addOutput(String name, OutputDefinitionDto output) {
        outputs.put(name, output);
        return this;
    }

    public WorkflowDefinitionBuilder retryPolicy(RetryPolicyDto policy) {
        this.retryPolicy = policy;
        return this;
    }

    public WorkflowDefinitionBuilder compensationPolicy(CompensationPolicyDto policy) {
        this.compensationPolicy = policy;
        return this;
    }

    public WorkflowDefinitionBuilder metadata(String key, String value) {
        metadata.put(key, value);
        return this;
    }

    public Uni<WorkflowDefinitionResponse> execute() {
        CreateWorkflowDefinitionRequest request = new CreateWorkflowDefinitionRequest(
                name,
                version,
                description,
                nodes,
                inputs,
                outputs,
                retryPolicy,
                compensationPolicy,
                metadata);
        return client.createDefinition(request);
    }
}
