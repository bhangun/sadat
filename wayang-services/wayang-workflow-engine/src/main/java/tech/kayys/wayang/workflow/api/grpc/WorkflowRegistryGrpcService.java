package tech.kayys.wayang.workflow.api.grpc;

import io.quarkus.grpc.GrpcService;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.workflow.service.WorkflowRegistry;
import tech.kayys.wayang.workflow.v1.ExportWorkflowResponse;
import tech.kayys.wayang.workflow.v1.GetWorkflowRequest;
import tech.kayys.wayang.workflow.v1.GetWorkflowVersionRequest;
import tech.kayys.wayang.workflow.v1.ImportWorkflowRequest;
import tech.kayys.wayang.workflow.v1.ListWorkflowsRequest;
import tech.kayys.wayang.workflow.v1.ListWorkflowsResponse;
import tech.kayys.wayang.workflow.v1.SearchWorkflowsRequest;
import tech.kayys.wayang.workflow.v1.ValidateWorkflowResponse;
import tech.kayys.wayang.workflow.v1.WorkflowDefinition;
import tech.kayys.wayang.workflow.v1.WorkflowRegistryService;
import tech.kayys.wayang.workflow.v1.WorkflowVersionId;
import tech.kayys.wayang.workflow.security.annotations.ControlPlaneSecured;
import com.google.protobuf.Empty;
import java.util.stream.Collectors;

@GrpcService
@ControlPlaneSecured
public class WorkflowRegistryGrpcService implements WorkflowRegistryService {

    private static final Logger LOG = Logger.getLogger(WorkflowRegistryGrpcService.class);

    @Inject
    WorkflowRegistry registry;

    @Inject
    SecurityIdentity securityIdentity;

    private String getTenantId() {
        if (securityIdentity.isAnonymous()) {
            throw new IllegalStateException("Anonymous access not allowed");
        }
        return securityIdentity.getPrincipal().getName();
    }

    @Override
    public Uni<WorkflowDefinition> registerWorkflow(WorkflowDefinition request) {
        LOG.infof("Registering workflow %s", request.getId());
        return registry.register(toDomain(request))
                .map(this::toProto);
    }

    @Override
    public Uni<WorkflowDefinition> getWorkflow(GetWorkflowRequest request) {
        return registry.getWorkflow(request.getWorkflowId())
                .map(def -> def != null ? toProto(def) : WorkflowDefinition.getDefaultInstance());
    }

    @Override
    public Uni<WorkflowDefinition> getWorkflowVersion(GetWorkflowVersionRequest request) {
        return registry.getWorkflowByVersion(request.getWorkflowId(), request.getVersion())
                .map(def -> def != null ? toProto(def) : WorkflowDefinition.getDefaultInstance());
    }

    @Override
    public Uni<ListWorkflowsResponse> listWorkflows(ListWorkflowsRequest request) {
        return registry.getAllWorkflows()
                .map(list -> ListWorkflowsResponse.newBuilder()
                        .addAllWorkflows(list.stream().map(this::toProto).collect(Collectors.toList()))
                        .build());
    }

    @Override
    public Uni<Empty> activateWorkflow(WorkflowVersionId request) {
        return registry.activate(request.getWorkflowId(), request.getVersion())
                .map(v -> Empty.getDefaultInstance());
    }

    @Override
    public Uni<Empty> deactivateWorkflow(WorkflowVersionId request) {
        return registry.deactivate(request.getWorkflowId(), request.getVersion())
                .map(v -> Empty.getDefaultInstance());
    }

    @Override
    public Uni<Empty> deleteWorkflow(WorkflowVersionId request) {
        return registry.delete(request.getWorkflowId(), request.getVersion())
                .map(v -> Empty.getDefaultInstance());
    }

    @Override
    public Uni<WorkflowDefinition> importWorkflow(ImportWorkflowRequest request) {
        return registry.importWorkflow(request.getId(), request.getContent())
                .map(this::toProto);
    }

    @Override
    public Uni<ExportWorkflowResponse> exportWorkflow(GetWorkflowRequest request) {
        return registry.exportWorkflow(request.getWorkflowId())
                .map(content -> ExportWorkflowResponse.newBuilder()
                        .setContent(content != null ? content : "")
                        .build());
    }

    @Override
    public Uni<ListWorkflowsResponse> searchWorkflows(SearchWorkflowsRequest request) {
        return registry.search(request.getTenantId(), request.getNamePattern())
                .map(list -> ListWorkflowsResponse.newBuilder()
                        .addAllWorkflows(list.stream().map(this::toProto).collect(Collectors.toList()))
                        .build());
    }

    @Override
    public Uni<ValidateWorkflowResponse> validateWorkflow(GetWorkflowRequest request) {
        return registry.validate(request.getWorkflowId())
                .map(valid -> ValidateWorkflowResponse.newBuilder().setValid(valid).build());
    }

    // Mappers

    private WorkflowDefinition toProto(tech.kayys.wayang.schema.workflow.WorkflowDefinition domain) {
        if (domain == null)
            return null;
        return WorkflowDefinition.newBuilder()
                .setId(domain.getId().getValue())
                .setVersion(domain.getVersion())
                .setName(domain.getName() != null ? domain.getName() : "")
                .setDescription(domain.getDescription() != null ? domain.getDescription() : "")
                .build();
    }

    private tech.kayys.wayang.schema.workflow.WorkflowDefinition toDomain(WorkflowDefinition proto) {
        return tech.kayys.wayang.schema.workflow.WorkflowDefinition.builder()
                .id(proto.getId())
                .version(proto.getVersion())
                .name(proto.getName())
                .description(proto.getDescription())
                .build();
    }
}
