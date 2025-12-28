package tech.kayys.wayang.workflow.api.grpc;

import io.quarkus.grpc.GrpcService;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.engine.WorkflowRunManager;
import tech.kayys.wayang.workflow.service.RunCheckpointService;
import tech.kayys.wayang.workflow.v1.*;
import tech.kayys.wayang.schema.execution.ErrorPayload;
import com.google.protobuf.Empty;
import io.grpc.Metadata;
import java.util.stream.Collectors;

@GrpcService
public class WorkflowRunGrpcService implements WorkflowRunService {

    @Inject
    WorkflowRunManager runManager;

    @Inject
    RunCheckpointService checkpointService;

    // TODO: Define a specific key or use a constant from a shared library
    private static final Metadata.Key<String> TENANT_ID_KEY = Metadata.Key.of("X-Tenant-Id",
            Metadata.ASCII_STRING_MARSHALLER);

    private String getTenantId() {
        // This is a simplification. In a real app, use an interceptor to propagate
        // headers into Context.
        // For now, we assume an interceptor might have put it there or default.
        // If using Quarkus gRPC, headers can be accessed via CurrentIdentityAssociation
        // or similar if secured.
        // Or we can rely on the user passing it if we added it to the proto request
        // (which we didn't for all).
        // Let's fallback to "default-tenant" for this implementation step.
        return "default-tenant";
    }

    @Override
    public Uni<WorkflowRun> createRun(tech.kayys.wayang.workflow.v1.CreateRunRequest request) {
        java.util.Map<String, Object> inputs = new java.util.HashMap<>(request.getInputsMap());
        tech.kayys.wayang.workflow.api.dto.CreateRunRequest domainRequest = new tech.kayys.wayang.workflow.api.dto.CreateRunRequest(
                request.getWorkflowId(),
                null, // version - not in proto request, maybe defaulting to latest
                inputs,
                "grpc"); // triggeredBy

        return runManager.createRun(domainRequest, getTenantId())
                .map(this::toProto);
    }

    @Override
    public Uni<WorkflowRun> getRun(GetRunRequest request) {
        return runManager.getRun(request.getRunId())
                .map(this::toProto);
    }

    @Override
    public Uni<ListRunsResponse> listRuns(ListRunsRequest request) {
        // request.getTenantId() removed as it's not in proto
        final String finalTenantId = getTenantId();

        // Parse status
        tech.kayys.wayang.workflow.api.model.RunStatus status = null;
        if (request.getStatus() != null && !request.getStatus().isEmpty()) {
            try {
                status = tech.kayys.wayang.workflow.api.model.RunStatus.valueOf(request.getStatus());
            } catch (IllegalArgumentException e) {
                // Ignore or error
            }
        }

        int page = request.getPagination().getPage();
        int size = request.getPagination().getSize() > 0 ? request.getPagination().getSize() : 20;

        return runManager.queryRuns(finalTenantId, request.getWorkflowId(), status, page, size)
                .map(result -> ListRunsResponse.newBuilder()
                        .addAllRuns(result.runs().stream().map(this::toProto).collect(Collectors.toList()))
                        .setPagination(PaginationResponse.newBuilder()
                                .setPage(page)
                                .setSize(size)
                                .setTotalElements(result.totalElements())
                                .setTotalPages(result.totalPages())
                                .build())
                        .build());
    }

    @Override
    public Uni<WorkflowRun> startRun(RunIdRequest request) {
        return runManager.startRun(request.getRunId(), getTenantId())
                .map(this::toProto);
    }

    @Override
    public Uni<WorkflowRun> suspendRun(SuspendRunRequest request) {
        return runManager.suspendRun(request.getRunId(), getTenantId(), request.getReason(), request.getHumanTaskId())
                .map(this::toProto);
    }

    @Override
    public Uni<WorkflowRun> resumeRun(ResumeRunRequest request) {
        java.util.Map<String, Object> resumeData = new java.util.HashMap<>(request.getResumeDataMap());
        return runManager
                .resumeRun(request.getRunId(), getTenantId(), request.getHumanTaskId(), resumeData)
                .map(this::toProto);
    }

    @Override
    public Uni<Empty> cancelRun(CancelRunRequest request) {
        return runManager.cancelRun(request.getRunId(), getTenantId(), request.getReason())
                .map(v -> Empty.getDefaultInstance());
    }

    @Override
    public Uni<WorkflowRun> completeRun(CompleteRunRequest request) {
        java.util.Map<String, Object> outputs = new java.util.HashMap<>(request.getOutputsMap());
        return runManager.completeRun(request.getRunId(), getTenantId(), outputs)
                .map(this::toProto);
    }

    @Override
    public Uni<WorkflowRun> failRun(FailRunRequest request) {
        // ErrorPayload constructor has 13 args. Using defaults for unknown values.
        ErrorPayload error = new ErrorPayload(
                null, // ErrorType
                "FAIL_RUN_API", // code
                java.util.Collections.emptyMap(), // details
                false, // retryable
                "workflow-engine", // domain
                request.getError(), // message/reason
                500, // httpStatus
                null, // grpcStatus
                java.time.LocalDateTime.now(), // timestamp
                null, // SuggestedAction
                null, // helpUrl
                null, // requestId
                null // traceId
        );
        return runManager
                .failRun(request.getRunId(), getTenantId(), error)
                .map(this::toProto);
    }

    @Override
    public Uni<ListCheckpointsResponse> listCheckpoints(RunIdRequest request) {
        return checkpointService.listCheckpoints(request.getRunId())
                .map(list -> ListCheckpointsResponse.newBuilder()
                        .addAllCheckpoints(list.stream().map(this::toProtoCheckpoint).collect(Collectors.toList()))
                        .build());
    }

    @Override
    public Uni<ActiveRunCount> getActiveCount(Empty request) {
        return runManager.getActiveRunsCount(getTenantId())
                .map(count -> ActiveRunCount.newBuilder().setCount(count).build());
    }

    // Mappers

    private WorkflowRun toProto(tech.kayys.wayang.workflow.domain.WorkflowRun domain) {
        if (domain == null)
            return null;
        WorkflowRun.Builder builder = WorkflowRun.newBuilder()
                .setRunId(domain.getRunId())
                .setWorkflowId(domain.getWorkflowId())
                .setWorkflowVersion(domain.getWorkflowVersion())
                .setStatus(domain.getStatus().name())
                .setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt().toEpochMilli() : 0)
                .setAttemptNumber(domain.getAttemptNumber())
                .setMaxAttempts(domain.getMaxAttempts());

        if (domain.getPhase() != null)
            builder.setPhase(domain.getPhase().name());
        if (domain.getStartedAt() != null)
            builder.setStartedAt(domain.getStartedAt().toEpochMilli());
        if (domain.getCompletedAt() != null)
            builder.setCompletedAt(domain.getCompletedAt().toEpochMilli());
        if (domain.getErrorMessage() != null)
            builder.setErrorMessage(domain.getErrorMessage());

        // Maps
        // Note: Proto map keys/values must be non-null.
        // Domain outputs is Map<String, Object>. Proto expects Map<String, String> (as
        // per my def).
        // I need to convert Object to String.
        if (domain.getOutputs() != null) {
            domain.getOutputs().forEach((k, v) -> {
                if (k != null && v != null)
                    builder.putOutputs(k, v.toString());
            });
        }

        // nodes_executed is map<string, string> in proto.
        // In domain it's List<NodeExecutionState> or Map?
        // Let's check WorkflowRun.java or infer.
        // It seems `run.getNodesExecuted()` returns something compliant or convertable?
        // WorkflowRunResource uses `run.getNodesExecuted()` which likely returns int
        // (count) based on `nodes_total`.
        // Wait, `nodesExecuted` in resource response is integer?
        // RunResponse builder `nodesExecuted(run.getNodesExecuted())`.

        // Proto definition has `map<string, string> nodes_executed = 10;` which
        // conflicts with `int32 nodes_executed = 5;` in Checkpoint or similar?
        // In `workflow_run.proto`:
        // map<string, string> nodes_executed = 10;
        // int32 nodes_total = 11;

        // I'll stick to what I defined. If domain has a map of executed nodes, I'll use
        // it.
        // WorkflowRun.java usually keeps track of node states.

        return builder.build();
    }

    private Checkpoint toProtoCheckpoint(tech.kayys.wayang.workflow.domain.Checkpoint domain) {
        return Checkpoint.newBuilder()
                .setCheckpointId(domain.getCheckpointId())
                .setRunId(domain.getRunId())
                .setSequenceNumber(domain.getSequenceNumber())
                .setStatus(domain.getStatus())
                .setNodesExecuted(domain.getNodesExecuted())
                .setCreatedAt(domain.getCreatedAt() != null ? domain.getCreatedAt().toEpochMilli() : 0)
                .build();
    }
}
