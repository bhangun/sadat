package tech.kayys.silat.sdk.client;

import java.util.List;

import io.smallrye.mutiny.Uni;

/**
 * REST-based workflow definition client
 */
public class RestWorkflowDefinitionClient implements WorkflowDefinitionClient {

    private final SilatClientConfig config;

    RestWorkflowDefinitionClient(SilatClientConfig config) {
        this.config = config;
    }

    // Implement methods...

    @Override
    public Uni<WorkflowDefinitionResponse> createDefinition(CreateWorkflowDefinitionRequest request) {
        return null;
    }

    @Override
    public Uni<WorkflowDefinitionResponse> getDefinition(String definitionId) {
        return null;
    }

    @Override
    public Uni<List<WorkflowDefinitionResponse>> listDefinitions(boolean activeOnly) {
        return null;
    }

    @Override
    public Uni<Void> deleteDefinition(String definitionId) {
        return null;
    }
}
