package tech.kayys.wayang.resources;

import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.websocket.service.WorkflowEventStream;

/**
 * SSE endpoint for workflow execution updates
 */
@Path("/api/v1/control-plane/events")
@Produces(MediaType.SERVER_SENT_EVENTS)
@Tag(name = "Control Plane - Events", description = "Real-time event streaming")
public class EventStreamResource {

    @Inject
    WorkflowEventStream eventStream;

    @GET
    @Path("/workflow/{runId}")
    @Operation(summary = "Stream workflow execution events")
    public io.smallrye.mutiny.Multi<String> streamWorkflowEvents(
            @PathParam("runId") String runId) {

        return eventStream.streamWorkflowEvents(runId);
    }

    @GET
    @Path("/agent/{agentId}")
    @Operation(summary = "Stream agent execution events")
    public io.smallrye.mutiny.Multi<String> streamAgentEvents(
            @PathParam("agentId") UUID agentId) {

        return eventStream.streamAgentEvents(agentId);
    }
}
