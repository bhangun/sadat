package tech.kayys.wayang.sdk;

import tech.kayys.wayang.sdk.dto.*;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * WorkflowRunClient: Primary SDK for workflow execution management.
 * 
 * Features:
 * - Trigger workflows
 * - Query run status
 * - Resume from HITL
 * - Cancel executions
 * - Inject events
 * - Stream execution updates (SSE)
 * 
 * Usage:
 * <pre>
 * @Inject
 * @RestClient
 * WorkflowRunClient client;
 * 
 * Uni<WorkflowRunResponse> result = client.triggerWorkflow(
 *     new TriggerWorkflowRequest("workflow-id", "tenant-id", "user:123", inputs)
 * );
 * </pre>
 */
@Path("/api/v1/runs")
@RegisterRestClient(configKey = "workflow-engine")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface WorkflowRunClient {

    /**
     * Trigger a new workflow execution
     * 
     * @param request Workflow trigger details
     * @return WorkflowRun response with runId
     */
    @POST
    Uni<WorkflowRunResponse> triggerWorkflow(TriggerWorkflowRequest request);

    /**
     * Trigger workflow synchronously (waits for completion)
     * 
     * @param request Workflow trigger details
     * @param timeoutMs Maximum wait time in milliseconds
     * @return Completed workflow run
     */
    /* Not implemented in Engine yet
    @POST
    @Path("/sync")
    Uni<WorkflowRunResponse> triggerWorkflowSync(
        TriggerWorkflowRequest request,
        @QueryParam("timeout") @DefaultValue("60000") long timeoutMs
    );
    */
    // Temporary shim if user code calls it, pointing to async trigger
    default Uni<WorkflowRunResponse> triggerWorkflowSync(
        TriggerWorkflowRequest request,
        long timeoutMs
    ) {
        return triggerWorkflow(request);
    }

    /**
     * Start a run
     */
    @POST
    @Path("/{runId}/start")
    Uni<WorkflowRunResponse> startRun(@PathParam("runId") String runId);

    /**
     * Suspend a run
     */
    @POST
    @Path("/{runId}/suspend")
    Uni<WorkflowRunResponse> suspendRun(@PathParam("runId") String runId, SuspendRequest request);

    /**
     * Get workflow run status
     * 
     * @param runId Workflow run identifier
     * @return Current run status
     */
    @GET
    @Path("/{runId}")
    Uni<WorkflowRunResponse> getWorkflowRun(@PathParam("runId") String runId);

    /* Not implemented in Engine yet
    @GET
    @Path("/{runId}/state")
    Uni<WorkflowRunStateResponse> getWorkflowRunState(@PathParam("runId") String runId);

    @GET
    @Path("/{runId}/history")
    Uni<List<NodeExecutionState>> getExecutionHistory(@PathParam("runId") String runId);
    */

    /**
     * Resume a waiting workflow (used by HITL service)
     * 
     * @param runId Workflow run identifier
     * @param request Resume details with correlation key and data
     * @return Updated workflow run
     */
    @POST
    @Path("/{runId}/resume")
    Uni<WorkflowRunResponse> resumeWorkflow(
        @PathParam("runId") String runId,
        ResumeWorkflowRequest request
    );

    /**
     * Cancel a running workflow
     * 
     * @param runId Workflow run identifier
     * @param request Cancellation details
     * @return Updated workflow run with CANCELLED status
     */
    @POST
    @Path("/{runId}/cancel")
    Uni<WorkflowRunResponse> cancelWorkflow(
        @PathParam("runId") String runId,
        CancelWorkflowRequest request
    );

    /* Not implemented in Engine yet
    @POST
    @Path("/{runId}/events")
    Uni<Void> injectEvent(
        @PathParam("runId") String runId,
        WorkflowEventRequest event
    );
    */

    /**
     * Query workflow runs with filters and pagination
     */
    @GET
    Uni<List<WorkflowRunResponse>> listWorkflowRuns(
        @QueryParam("workflowId") String workflowId,
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("20") int size
    );

    /* Not implemented in Engine yet
    @GET
    @Path("/{runId}/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    Multi<WorkflowExecutionEvent> streamExecution(@PathParam("runId") String runId);

    @POST
    @Path("/{runId}/retry")
    Uni<WorkflowRunResponse> retryWorkflow(@PathParam("runId") String runId);

    @GET
    @Path("/{runId}/output")
    Uni<Map<String, Object>> getWorkflowOutput(@PathParam("runId") String runId);

    @PATCH
    @Path("/{runId}/variables")
    Uni<Void> updateWorkflowVariables(
        @PathParam("runId") String runId,
        Map<String, Object> variables
    );
    */
}
