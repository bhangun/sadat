package tech.kayys.wayang.workflow.api.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.service.NodeRegistry;
import tech.kayys.wayang.workflow.v1.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import tech.kayys.wayang.workflow.security.annotations.ExecutionSecured;

@GrpcService
@ExecutionSecured
public class NodeRegistryGrpcService implements NodeRegistryService {

    @Inject
    NodeRegistry nodeRegistry;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public Uni<ListNodeTypesResponse> listNodeTypes(ListNodeTypesRequest request) {
        return nodeRegistry.getRegisteredNodeTypes()
                .map(types -> ListNodeTypesResponse.newBuilder()
                        .addAllTypes(types)
                        .build());
    }

    @Override
    public Uni<NodeTypeSchema> getNodeTypeInfo(GetNodeTypeInfoRequest request) {
        return nodeRegistry.getNodeTypeSchema(request.getType())
                .map(schema -> {
                    String json = "{}";
                    try {
                        json = objectMapper.writeValueAsString(schema);
                    } catch (Exception e) {
                        // ignore
                    }
                    return NodeTypeSchema.newBuilder()
                            .setJsonSchema(json)
                            .build();
                });
    }

    @Override
    public Uni<ValidateNodeResponse> validateNode(ValidateNodeRequest request) {
        // Stub implementation
        return Uni.createFrom().item(ValidateNodeResponse.newBuilder()
                .setValid(true)
                .setMessage("Validation stubbed")
                .build());
    }
}
