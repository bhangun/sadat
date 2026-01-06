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
import tech.kayys.wayang.workflow.kernel.WorkflowRunManager;
import tech.kayys.wayang.workflow.api.dto.*;
import tech.kayys.wayang.workflow.api.model.RunStatus;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.Map;

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

        @Context
        HttpHeaders httpHeaders;

        /**
         * Create a new workflow run
         */
        @POST
        @Operation(summary = "Create workflow run", description = "Create a new workflow execution instance")
        public Uni<Response> createRun(@Valid CreateRunRequest request) {
                String tenantId = getTenantId();
                LOG.infof("Creating run for workflow: %s, tenant: %s", request.getWorkflowId(), tenantId);

                return runManager.createRun(request, tenantId)
                                .map(run -> Response
                                                .status(Response.Status.CREATED)
                                                .entity(toResponse(run))
                                                .build())
                                .onFailure()
                                .recoverWithItem(th -> mapError(th, Response.Status.BAD_REQUEST, "CREATE_FAILED"));
        }

        /**
         * Get run details
         */
        @GET
        @Path("/{runId}")
        @Operation(summary = "Get run details", description = "Retrieve workflow run details by ID")
        public Uni<Response> getRun(@PathParam("runId") String runId) {
                LOG.infof("Getting run details: %s", runId);
                return runManager.getRun(runId)
                                .map(run -> Response.ok(toResponse(run)).build())
                                .onFailure()
                                .recoverWithItem(th -> mapError(th, Response.Status.NOT_FOUND, "RUN_NOT_FOUND"));
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
                LOG.infof("Listing runs: workflow=%s, status=%s", workflowId, statusStr);
                String tenantId = getTenantId();

                return Uni.createFrom().item(() -> {
                        try {
                                return statusStr != null ? RunStatus.valueOf(statusStr) : null;
                        } catch (IllegalArgumentException e) {
                                throw new BadRequestException("Invalid run status: " + statusStr);
                        }
                }).chain(status -> runManager.queryRuns(tenantId, workflowId, status, page, size))
                                .map(response -> Response.ok(response).build())
                                .onFailure()
                                .recoverWithItem(th -> mapError(th, Response.Status.BAD_REQUEST, "QUERY_FAILED"));
        }

        /**
         * Start a run
         */
        @POST
        @Path("/{runId}/start")
        @Operation(summary = "Start run", description = "Start workflow execution")
        public Uni<Response> startRun(@PathParam("runId") String runId) {
                LOG.infof("Starting run: %s", runId);
                String tenantId = getTenantId();

                return runManager.startRun(runId, tenantId)
                                .map(run -> Response.ok(toResponse(run)).build())
                                .onFailure()
                                .recoverWithItem(th -> mapError(th, Response.Status.BAD_REQUEST, "START_FAILED"));
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
                                .onFailure()
                                .recoverWithItem(th -> mapError(th, Response.Status.BAD_REQUEST, "SUSPEND_FAILED"));
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
                                .onFailure()
                                .recoverWithItem(th -> mapError(th, Response.Status.BAD_REQUEST, "RESUME_FAILED"));
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
                                .onFailure()
                                .recoverWithItem(th -> mapError(th, Response.Status.BAD_REQUEST, "CANCEL_FAILED"));
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
                                .onFailure()
                                .recoverWithItem(th -> mapError(th, Response.Status.BAD_REQUEST, "COMPLETE_FAILED"));
        }

        /**
         * Fail a run
         */
        @POST
        @Path("/{runId}/fail")
        @Operation(summary = "Fail run", description = "Fail workflow execution with error")
        public Uni<Response> failRun(
                        @PathParam("runId") String runId,
                        @Valid ErrorResponse request) {
                LOG.infof("Failing run: %s", runId);
                String tenantId = getTenantId();

                return runManager.failRun(runId, tenantId, request)
                                .map(run -> Response.ok(toResponse(run)).build())
                                .onFailure()
                                .recoverWithItem(th -> mapError(th, Response.Status.BAD_REQUEST, "FAIL_FAILED"));
        }

        /**
         * List checkpoints for a run
         */
        @GET
        @Path("/{runId}/checkpoints")
        @Operation(summary = "List checkpoints", description = "List execution checkpoints")
        public Uni<Response> listCheckpoints(@PathParam("runId") String runId) {
                LOG.infof("Listing checkpoints: %s", runId);
                return checkpointService.listCheckpoints(runId)
                                .map(checkpoints -> checkpoints.stream()
                                                .map(this::toCheckpointResponse)
                                                .toList())
                                .map(items -> Response.ok(items).build())
                                .onFailure().recoverWithItem(th -> mapError(th, Response.Status.INTERNAL_SERVER_ERROR,
                                                "CHECKPOINT_QUERY_FAILED"));
        }

        /**
         * Get active runs count
         */
        @GET
        @Path("/active/count")
        @Operation(summary = "Get active runs count", description = "Get count of active runs")
        public Uni<Response> getActiveCount() {
                String tenantId = getTenantId();
                LOG.infof("Getting active runs count: tenant=%s", tenantId);
                return runManager.getActiveRunsCount(tenantId)
                                .map(count -> Response.ok(Map.of("count", count)).build())
                                .onFailure().recoverWithItem(th -> mapError(th, Response.Status.INTERNAL_SERVER_ERROR,
                                                "COUNT_FAILED"));
        }

        // Helper methods

        private String getTenantId() {
                // 1. Try X-Tenant-Id header
                String headerTenantId = httpHeaders.getHeaderString("X-Tenant-Id");
                if (headerTenantId != null && !headerTenantId.trim().isEmpty()) {
                        return headerTenantId;
                }

                // 2. Try SecurityContext principal name
                if (securityContext != null && securityContext.getUserPrincipal() != null) {
                        return securityContext.getUserPrincipal().getName();
                }

                // Default fallback
                return "default-tenant";
        }

        private Response mapError(Throwable th, Response.Status status, String errorCode) {
                LOG.errorf(th, "Request failed: %s - %s", errorCode, th.getMessage());
                return Response.status(status)
                                .entity(ErrorResponse.builder()
                                                .errorCode(errorCode)
                                                .message(th.getMessage())
                                                .timestamp(java.time.Instant.now())
                                                .build())
                                .build();
        }

        // Mappers

        private RunResponse toResponse(WorkflowRun run) {
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