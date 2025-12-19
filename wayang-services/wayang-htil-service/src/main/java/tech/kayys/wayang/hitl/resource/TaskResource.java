package tech.kayys.wayang.node.resource;

import java.util.Map;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Human Task REST API.
 */
@Path("/api/v1/tasks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class TaskResource {

    @Inject
    HTILService htilService;

    @Context
    SecurityContext securityContext;

    @GET
    @Path("/my-tasks")
    public Uni<Response> getMyTasks() {
        String operatorId = securityContext.getUserPrincipal().getName();

        return htilService.getTasksForOperator(operatorId)
                .map(tasks -> Response.ok(tasks).build());
    }

    @GET
    @Path("/{taskId}")
    public Uni<Response> getTask(@PathParam("taskId") String taskId) {
        return htilService.getTask(taskId)
                .map(task -> Response.ok(task).build())
                .onItem().ifNull().continueWith(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path("/{taskId}/complete")
    public Uni<Response> completeTask(
            @PathParam("taskId") String taskId,
            io.agentic.platform.hitl.HTILTaskResult result) {

        String operatorId = securityContext.getUserPrincipal().getName();

        return htilService.completeTask(taskId, result, operatorId)
                .map(task -> Response.ok(task).build())
                .onFailure().recoverWithItem(th -> Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", th.getMessage()))
                        .build());
    }
}
