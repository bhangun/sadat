package tech.kayys.silat.sdk.client;

import java.util.List;

import io.smallrye.mutiny.Uni;

/**
 * Workflow definition client interface
 */
interface WorkflowDefinitionClient {
    Uni<WorkflowDefinitionResponse> createDefinition(CreateWorkflowDefinitionRequest request);

    Uni<WorkflowDefinitionResponse> getDefinition(String definitionId);

    Uni<List<WorkflowDefinitionResponse>> listDefinitions(boolean activeOnly);

    Uni<Void> deleteDefinition(String definitionId);
}
