package tech.kayys.wayang.resource;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;
import tech.kayys.wayang.schema.ExecutionRequest;
import tech.kayys.wayang.schema.ExecutionStatus;
import tech.kayys.wayang.service.WorkflowExecutionService;
import tech.kayys.wayang.sdk.dto.*;
import tech.kayys.wayang.tenant.TenantContext;

import java.util.Collections;
import java.util.List;

/**
 * WorkflowRunResource - Implementation of WorkflowRunClient interface
 */
@Path("/api/v1/runs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkflowRunResource {

    private static final Logger LOG = Logger.getLogger(WorkflowRunResource.class);

    @Inject
    WorkflowExecutionService executionService;

    @Inject
    TenantContext tenantContext;

    @POST
    public Uni<WorkflowRunResponse> triggerWorkflow(TriggerWorkflowRequest request) {
        LOG.infof("Triggering workflow %s (v%s)", request.workflowId(), request.workflowVersion());

        ExecutionRequest execRequest = new ExecutionRequest();
        execRequest.setInputs(request.inputs());
        execRequest.setAsync(true); // Default to async as per SDK pattern
        if (request.correlationId() != null) {
            execRequest.getContext().put("correlationId", request.correlationId());
        }
        
        // Pass version if possible, assume it's part of context or resolved by ID in service
        if (request.workflowVersion() != null) {
            execRequest.getContext().put("workflowVersion", request.workflowVersion());
        }

        return executionService.execute(request.workflowId(), execRequest, tenantContext.getTenantId())
                .map(this::toRunResponse);
    }

    @POST
    @Path("/{runId}/start")
    public Uni<WorkflowRunResponse> startRun(@PathParam("runId") String runId) {
        // Since triggerWorkflow starts execution, this might be a no-op or specific to restarting
        // For now, we fetch status to confirm it exists, or maybe we re-trigger if needed.
        // Given current service capabilities, we just return current status.
        return executionService.getStatus(runId, tenantContext.getTenantId())
                .map(this::toRunResponse);
    }

    @POST
    @Path("/{runId}/suspend")
    public Uni<WorkflowRunResponse> suspendRun(@PathParam("runId") String runId, SuspendRequest request) {
        // Not implemented in service yet
        return Uni.createFrom().failure(new WebApplicationException("Suspend not implemented", 501));
    }

    @GET
    @Path("/{runId}")
    public Uni<WorkflowRunResponse> getWorkflowRun(@PathParam("runId") String runId) {
        return executionService.getStatus(runId, tenantContext.getTenantId())
                .map(this::toRunResponse);
    }

    @POST
    @Path("/{runId}/resume")
    public Uni<WorkflowRunResponse> resumeWorkflow(@PathParam("runId") String runId, ResumeWorkflowRequest request) {
        // Not implemented in service yet
        return Uni.createFrom().failure(new WebApplicationException("Resume not implemented", 501));
    }

    @POST
    @Path("/{runId}/cancel")
    public Uni<WorkflowRunResponse> cancelWorkflow(@PathParam("runId") String runId, CancelWorkflowRequest request) {
        return executionService.cancel(runId, tenantContext.getTenantId())
                .flatMap(success -> executionService.getStatus(runId, tenantContext.getTenantId()))
                .map(this::toRunResponse);
    }

    @GET
    public Uni<List<WorkflowRunResponse>> listWorkflowRuns(
            @QueryParam("workflowId") String workflowId,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        // Not implemented in service yet
        // Returning empty list
        return Uni.createFrom().item(Collections.emptyList());
    }

    private WorkflowRunResponse toRunResponse(ExecutionStatus status) {
        RunStatus runStatus = mapStatus(status.getStatus());
        
        return new WorkflowRunResponse(
            status.getId(),
            status.getWorkflowId(),
            "latest", // Version 
            runStatus,
            "Execute", // Phase
            status.getStartedAt(), // Created at (approx)
            status.getStartedAt(),
            status.getCompletedAt(),
            status.getDuration() != null ? status.getDuration().toMillis() : null,
            status.getNodeStatuses() != null ? status.getNodeStatuses().size() : 0,
            0, // total nodes unknown
            1, // attempt
            1, // max attempts
            status.getError() != null ? status.getError().toString() : null,
            status.getOutputs()
        );
    }
    
    private RunStatus mapStatus(tech.kayys.wayang.schema.ExecutionStatusEnum status) {
        if (status == null) return RunStatus.PENDING;
        
        switch (status) {
            case PENDING: return RunStatus.PENDING;
            case RUNNING: return RunStatus.RUNNING;
            case PAUSED: return RunStatus.PAUSED;
            case COMPLETED: return RunStatus.SUCCEEDED; // Map Engine COMPLETED to SDK SUCCEEDED
            case FAILED: return RunStatus.FAILED;
            case CANCELLED: return RunStatus.CANCELLED;
            case TIMEOUT: return RunStatus.TIMED_OUT; // Map Engine TIMEOUT to SDK TIMED_OUT
            default: return RunStatus.PENDING;
        }
    }
}
