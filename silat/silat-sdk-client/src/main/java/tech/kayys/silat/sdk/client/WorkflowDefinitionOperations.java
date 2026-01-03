package tech.kayys.silat.sdk.client;

import java.util.List;

import io.smallrye.mutiny.Uni;

/**
 * Fluent API for workflow definition operations
 */
public class WorkflowDefinitionOperations {

    private final WorkflowDefinitionClient client;

    WorkflowDefinitionOperations(WorkflowDefinitionClient client) {
        this.client = client;
    }

    /**
     * Create a new workflow definition
     */
    public WorkflowDefinitionBuilder create(String name) {
        return new WorkflowDefinitionBuilder(client, name);
    }

    /**
     * Get a workflow definition
     */
    public Uni<WorkflowDefinitionResponse> get(String definitionId) {
        return client.getDefinition(definitionId);
    }

    /**
     * List workflow definitions
     */
    public Uni<List<WorkflowDefinitionResponse>> list() {
        return client.listDefinitions(true);
    }

    /**
     * Delete a workflow definition
     */
    public Uni<Void> delete(String definitionId) {
        return client.deleteDefinition(definitionId);
    }
}