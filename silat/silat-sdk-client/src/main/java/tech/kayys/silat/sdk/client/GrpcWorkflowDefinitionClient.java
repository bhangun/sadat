package tech.kayys.silat.sdk.client;

/**
 * gRPC-based workflow definition client
 */
class GrpcWorkflowDefinitionClient implements WorkflowDefinitionClient {

    private final SilatClientConfig config;

    GrpcWorkflowDefinitionClient(SilatClientConfig config) {
        this.config = config;
    }

    // Implement using gRPC stubs...

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
