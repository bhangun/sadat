package tech.kayys.wayang.workflow.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import tech.kayys.wayang.workflow.domain.Checkpoint;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.service.RunCheckpointService;
import tech.kayys.wayang.workflow.engine.WorkflowRunManager;
import tech.kayys.wayang.workflow.api.dto.*;
import tech.kayys.wayang.workflow.api.model.RunStatus;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.List;

/**
 * WorkflowRunResource - REST API for workflow run management
 * 
 * Endpoints:
 * - POST /api/v1/runs - Create run
 * - GET /api/v1/runs/{runId} - Get run details
 * - GET /api/v1/runs - List runs
 * - POST /api/v1/runs/{runId}/start - Start run
 * - POST /api/v1/runs/{runId}/suspend - Suspend run
 * - POST /api/v1/runs/{runId}/resume - Resume run
 * - POST /api/v1/runs/{runId}/cancel - Cancel run
 * - GET /api/v1/runs/{runId}/checkpoints - List checkpoints
 */
@Path("/api/v1/runs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Workflow Runs", description = "Workflow execution management")
public class WorkflowRunResource {

        private static final Logger LOG = Logger.getLogger(WorkflowRunResource.class);

        @Inject
        WorkflowRunManager runManager;

        @Inject
        RunCheckpointService checkpointService;

        @Context
        SecurityContext securityContext;

        /**
         * Create a new workflow run
         */
        @POST
        @Operation(summary = "Create workflow run", description = "Create a new workflow execution instance")
        public Uni<Response> createRun(@Valid CreateRunRequest request) {
                LOG.info("Creating run for workflow: " + request.getWorkflowId());
                String tenantId = getTenantId();
                // Assuming user ID extraction needed for request if not in DTO, or Manager
                // handles it
                // The current createRun in Manager takes CreateRunRequest.

                LOG.infof("Creating run for workflow: %s", request.getWorkflowId());

                return runManager.createRun(request)
                                .map(run -> Response
                                                .status(Response.Status.CREATED)
                                                .entity(toResponse(run))
                                                .build())
                                .onFailure().recoverWithItem(th -> {
                                        LOG.error("Failed to create run", th);
                                        return Response
                                                        .status(Response.Status.BAD_REQUEST)
                                                        .entity(ErrorResponse.builder()
                                                                        .errorCode("CREATE_FAILED")
                                                                        .message(th.getMessage())
                                                                        .timestamp(java.time.Instant.now())
                                                                        .build())
                                                        .build();
                                });
        }

        /**
         * Get run details
         */
        @GET
        @Path("/{runId}")
        @Operation(summary = "Get run details", description = "Retrieve workflow run details by ID")
        public Uni<Response> getRun(@PathParam("runId") String runId) {
                LOG.info("Getting run details for run ID: " + runId);
                return runManager.getRun(runId)
                                .map(run -> Response.ok(toResponse(run)).build())
                                .onFailure().recoverWithItem(th -> Response.status(Response.Status.NOT_FOUND)
                                                .entity(ErrorResponse.builder()
                                                                .message(th.getMessage())
                                                                .timestamp(java.time.Instant.now())
                                                                .build())
                                                .build());
        }

        /**
         * List runs by workflow
         */
        @GET
        @Operation(summary = "List runs", description = "List workflow runs with pagination")
        public Uni<Response> listRuns(
                        @QueryParam("workflowId") String workflowId,
                        @QueryParam("status") String statusStr,
                        @QueryParam("page") @DefaultValue("0") int page,
                        @QueryParam("size") @DefaultValue("20") int size) {
                LOG.info("Listing runs for workflow ID: " + workflowId + " and status: " + statusStr);
                String tenantId = getTenantId();
                RunStatus status = statusStr != null
                                ? RunStatus.valueOf(statusStr)
                                : null;

                return runManager.queryRuns(tenantId, workflowId, status, page, size)
                                .map(response -> Response.ok(response).build());
        }

        /**
         * Start a run
         */
        @POST
        @Path("/{runId}/start")
        @Operation(summary = "Start run", description = "Start workflow execution")
        public Uni<Response> startRun(@PathParam("runId") String runId) {
                String tenantId = getTenantId();

                LOG.infof("Starting run: %s", runId);

                return runManager.startRun(runId, tenantId)
                                .map(run -> Response.ok(toResponse(run)).build())
                                .onFailure().recoverWithItem(th -> {
                                        LOG.error("Failed to start run", th);
                                        return Response
                                                        .status(Response.Status.BAD_REQUEST)
                                                        .entity(ErrorResponse.builder()
                                                                        .message(th.getMessage())
                                                                        .build())
                                                        .build();
                                });
        }

        /**
         * Suspend a run
         */
        @POST
        @Path("/{runId}/suspend")
        @Operation(summary = "Suspend run", description = "Suspend workflow execution")
        public Uni<Response> suspendRun(
                        @PathParam("runId") String runId,
                        @Valid SuspendRequest request) {
                LOG.infof("Suspending run: %s", runId);
                String tenantId = getTenantId();

                return runManager.suspendRun(
                                runId,
                                tenantId,
                                request.getReason(),
                                request.getHumanTaskId())
                                .map(run -> Response.ok(toResponse(run)).build())
                                .onFailure().recoverWithItem(th -> Response.status(Response.Status.BAD_REQUEST)
                                                .entity(ErrorResponse.builder().message(th.getMessage()).build())
                                                .build());
        }

        /**
         * Resume a run
         */
        @POST
        @Path("/{runId}/resume")
        @Operation(summary = "Resume run", description = "Resume suspended workflow execution")
        public Uni<Response> resumeRun(
                        @PathParam("runId") String runId,
                        @Valid ResumeRequest request) {
                LOG.infof("Resuming run: %s", runId);
                String tenantId = getTenantId();

                return runManager.resumeRun(
                                runId,
                                tenantId,
                                request.getHumanTaskId(),
                                request.getResumeData())
                                .map(run -> Response.ok(toResponse(run)).build())
                                .onFailure().recoverWithItem(th -> Response.status(Response.Status.BAD_REQUEST)
                                                .entity(ErrorResponse.builder().message(th.getMessage()).build())
                                                .build());
        }

        /**
         * Cancel a run
         */
        @POST
        @Path("/{runId}/cancel")
        @Operation(summary = "Cancel run", description = "Cancel workflow execution")
        public Uni<Response> cancelRun(
                        @PathParam("runId") String runId,
                        @Valid CancelRequest request) {
                LOG.infof("Canceling run: %s", runId);
                String tenantId = getTenantId();

                return runManager.cancelRun(runId, tenantId, request.getReason())
                                .map(v -> Response.noContent().build())
                                .onFailure().recoverWithItem(th -> Response.status(Response.Status.BAD_REQUEST)
                                                .entity(ErrorResponse.builder().message(th.getMessage()).build())
                                                .build());
        }

        /**
         * Complete a run
         */
        @POST
        @Path("/{runId}/complete")
        @Operation(summary = "Complete run", description = "Complete workflow execution successfully")
        public Uni<Response> completeRun(
                        @PathParam("runId") String runId,
                        @Valid CompleteRunRequest request) {
                LOG.infof("Completing run: %s", runId);
                String tenantId = getTenantId();

                return runManager.completeRun(runId, tenantId, request.getOutputs())
                                .map(run -> Response.ok(toResponse(run)).build())
                                .onFailure().recoverWithItem(th -> Response.status(Response.Status.BAD_REQUEST)
                                                .entity(ErrorResponse.builder().message(th.getMessage()).build())
                                                .build());
        }

        /**
         * Fail a run
         */
        @POST
        @Path("/{runId}/fail")
        @Operation(summary = "Fail run", description = "Fail workflow execution with error")
        public Uni<Response> failRun(
                        @PathParam("runId") String runId,
                        @Valid FailRunRequest request) {
                LOG.infof("Failing run: %s", runId);
                String tenantId = getTenantId();

                return runManager.failRun(runId, tenantId, request.getError())
                                .map(run -> Response.ok(toResponse(run)).build())
                                .onFailure().recoverWithItem(th -> Response.status(Response.Status.BAD_REQUEST)
                                                .entity(ErrorResponse.builder().message(th.getMessage()).build())
                                                .build());
        }

        /**
         * List checkpoints for a run
         */
        @GET
        @Path("/{runId}/checkpoints")
        @Operation(summary = "List checkpoints", description = "List execution checkpoints")
        public Uni<List<CheckpointResponse>> listCheckpoints(@PathParam("runId") String runId) {
                LOG.infof("Listing checkpoints for run: %s", runId);
                return checkpointService.listCheckpoints(runId)
                                .map(checkpoints -> checkpoints.stream()
                                                .map(this::toCheckpointResponse)
                                                .toList());
        }

        /**
         * Get active runs count
         */
        @GET
        @Path("/active/count")
        @Operation(summary = "Get active runs count", description = "Get count of active runs")
        public Uni<Response> getActiveCount() {
                String tenantId = getTenantId();
                LOG.infof("Getting active runs count for tenant: %s", tenantId);
                return runManager.getActiveRunsCount(tenantId)
                                .map(count -> Response.ok(Map.of("count", count)).build());
        }

        // Helper methods

        private String getTenantId() {
                // Extract from JWT or security context
                // For now, return default
                return "default-tenant";
        }

        private String getUserId() {
                return securityContext.getUserPrincipal().getName();
        }

        // Mappers

        private RunResponse toResponse(WorkflowRun run) {
                LOG.infof("Converting run to response: %s", run.getRunId());
                return RunResponse.builder()
                                .runId(run.getRunId())
                                .workflowId(run.getWorkflowId())
                                .workflowVersion(run.getWorkflowVersion())
                                .status(run.getStatus().name())
                                .phase(run.getPhase() != null ? run.getPhase().name() : null)
                                .createdAt(run.getCreatedAt())
                                .startedAt(run.getStartedAt())
                                .completedAt(run.getCompletedAt())
                                .durationMs(run.getDurationMs())
                                .nodesExecuted(run.getNodesExecuted())
                                .nodesTotal(run.getNodesTotal())
                                .attemptNumber(run.getAttemptNumber())
                                .maxAttempts(run.getMaxAttempts())
                                .errorMessage(run.getErrorMessage())
                                .outputs(run.getOutputs())
                                .build();
        }

        private CheckpointResponse toCheckpointResponse(Checkpoint checkpoint) {
                LOG.infof("Converting checkpoint to response: %s", checkpoint.getCheckpointId());
                return CheckpointResponse.builder()
                                .checkpointId(checkpoint.getCheckpointId())
                                .runId(checkpoint.getRunId())
                                .sequenceNumber(checkpoint.getSequenceNumber())
                                .status(checkpoint.getStatus())
                                .nodesExecuted(checkpoint.getNodesExecuted())
                                .createdAt(checkpoint.getCreatedAt())
                                .build();
        }
}