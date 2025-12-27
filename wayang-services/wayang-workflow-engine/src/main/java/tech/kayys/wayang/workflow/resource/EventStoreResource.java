package tech.kayys.wayang.workflow.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import tech.kayys.wayang.workflow.service.WorkflowEventStore;
import tech.kayys.wayang.workflow.api.dto.ErrorResponse;

/**
 * EventStoreResource - REST API for raw event store access
 */
@Path("/api/v1/events")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Event Store", description = "Raw workflow event access")
public class EventStoreResource {

        private static final Logger LOG = Logger.getLogger(EventStoreResource.class);

        @Inject
        WorkflowEventStore eventStore;

        @GET
        @Path("/runs/{runId}")
        @Operation(summary = "Get run events", description = "Get all events for a workflow run")
        public Uni<Response> getRunEvents(@PathParam("runId") String runId) {
                LOG.info("Getting events for run: " + runId);
                return eventStore.getEvents(runId)
                                .map(events -> Response.ok(events).build())
                                .onFailure()
                                .recoverWithItem(th -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                                .entity(new ErrorResponse("EVENT_FETCH_FAILED", th.getMessage()))
                                                .build());
        }

        @GET
        @Path("/runs/{runId}/types/{eventType}")
        @Operation(summary = "Get events by type", description = "Get events of specific type for a run")
        public Uni<Response> getEventsByType(
                        @PathParam("runId") String runId,
                        @PathParam("eventType") String eventType) {
                LOG.info("Getting events for run: " + runId + " and type: " + eventType);
                return eventStore.getEventsByType(runId, eventType)
                                .map(events -> Response.ok(events).build())
                                .onFailure()
                                .recoverWithItem(th -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                                .entity(new ErrorResponse("EVENT_FETCH_FAILED", th.getMessage()))
                                                .build());
        }
}
