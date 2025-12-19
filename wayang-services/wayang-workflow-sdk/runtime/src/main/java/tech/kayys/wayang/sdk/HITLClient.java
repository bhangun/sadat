package tech.kayys.wayang.sdk;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import tech.kayys.wayang.sdk.dto.*;
import tech.kayys.wayang.sdk.dto.htil.*; // For BITL specific DTOs if any


import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Client for Human-in-the-Loop task management
 */
@Path("/api/v1/tasks")
@RegisterRestClient(configKey = "htil-service")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface HITLClient {

    /**
     * Get pending tasks for operator
     */
    @GET
    Uni<List<HumanTaskResponse>> getPendingTasks(
        @QueryParam("operatorId") String operatorId,
        @QueryParam("priority") TaskPriority priority
    );

    /**
     * Get task by ID
     */
    @GET
    @Path("/{taskId}")
    Uni<HumanTaskResponse> getTask(@PathParam("taskId") String taskId);

    /**
     * Complete a human task
     */
    @POST
    @Path("/{taskId}/complete")
    Uni<Void> completeTask(
        @PathParam("taskId") String taskId,
        TaskCompletionRequest request
    );

    /**
     * Reassign task to another operator
     */
    @POST
    @Path("/{taskId}/reassign")
    Uni<Void> reassignTask(
        @PathParam("taskId") String taskId,
        @QueryParam("newOperatorId") String newOperatorId
    );

    /**
     * Add comment to task
     */
    @POST
    @Path("/{taskId}/comments")
    Uni<Void> addComment(
        @PathParam("taskId") String taskId,
        TaskCommentRequest comment
    );

    /**
     * Escalate task to supervisor
     */
    @POST
    @Path("/{taskId}/escalate")
    Uni<Void> escalateTask(@PathParam("taskId") String taskId, String reason);
}
