package tech.kayys.wayang.workflow.api.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.service.WorkflowComposer;
import tech.kayys.wayang.workflow.v1.*;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;

@GrpcService
public class WorkflowComposerGrpcService implements WorkflowComposerService {

    @Inject
    WorkflowComposer composer;

    @Override
    public Uni<tech.kayys.wayang.workflow.v1.WorkflowDefinition> composeWorkflows(ComposeRequest request) {
        return composer.compose(request.getParentId(), request.getChildIdsList())
                .map(this::toProto);
    }

    @Override
    public Uni<tech.kayys.wayang.workflow.v1.WorkflowDefinition> createForkJoin(ForkJoinRequest request) {
        return Uni.createFrom().item(
                composer.forkJoin(request.getId(), request.getBranchIdsList()))
                .map(this::toProto);
    }

    @Override
    public Uni<tech.kayys.wayang.workflow.v1.WorkflowDefinition> createSequential(SequentialRequest request) {
        return composer.sequential(request.getId(), request.getStepIdsList())
                .map(this::toProto);
    }

    @Override
    public Uni<tech.kayys.wayang.workflow.v1.WorkflowDefinition> createConditional(ConditionalRequest request) {
        return Uni.createFrom().item(
                composer.conditional(request.getId(), request.getNodeIdsList()))
                .map(this::toProto);
    }

    // Mapper

    private tech.kayys.wayang.workflow.v1.WorkflowDefinition toProto(WorkflowDefinition domain) {
        if (domain == null)
            return null;
        return tech.kayys.wayang.workflow.v1.WorkflowDefinition.newBuilder()
                .setId(domain.getId().getValue())
                .setVersion(domain.getVersion())
                .setName(domain.getName() != null ? domain.getName() : "")
                .setDescription(domain.getDescription() != null ? domain.getDescription() : "")
                .build();
    }
}
