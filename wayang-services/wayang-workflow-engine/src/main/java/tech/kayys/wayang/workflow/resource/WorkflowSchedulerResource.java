package tech.kayys.wayang.workflow.resource;

import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.workflow.scheduler.dto.ScheduleRequest;
import tech.kayys.wayang.workflow.scheduler.dto.ScheduleResponse;
import tech.kayys.wayang.workflow.scheduler.dto.ScheduleUpdateRequest;
import tech.kayys.wayang.workflow.scheduler.model.WorkflowSchedule;
import tech.kayys.wayang.workflow.scheduler.service.WorkflowScheduler;

import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

/**
 * WorkflowSchedulerResource: REST API for workflow scheduling.
 * 
 * Endpoints:
 * - POST /api/v1/schedules - Create schedule
 * - GET /api/v1/schedules - List schedules
 * - GET /api/v1/schedules/{id} - Get schedule
 * - PUT /api/v1/schedules/{id} - Update schedule
 * - DELETE /api/v1/schedules/{id} - Delete schedule
 * - GET /api/v1/schedules/{id}/history - Get execution history
 * - POST /api/v1/schedules/{id}/trigger - Trigger immediately
 */
@Path("/api/v1/schedules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkflowSchedulerResource {

        private static final Logger LOG = Logger.getLogger(WorkflowSchedulerResource.class);

        @Inject
        WorkflowScheduler scheduler;

        /**
         * Create a new workflow schedule.
         */
        @POST
        public Uni<Response> createSchedule(ScheduleRequest request) {
                LOG.infof("Creating schedule for workflow %s", request.workflowId());

                return scheduler.createSchedule(request)
                                .map(schedule -> Response
                                                .status(Response.Status.CREATED)
                                                .entity(toScheduleResponse(schedule))
                                                .build())
                                .onFailure().recoverWithItem(error -> {
                                        LOG.error("Failed to create schedule", error);
                                        return Response
                                                        .status(Response.Status.BAD_REQUEST)
                                                        .entity(Map.of("error", error.getMessage()))
                                                        .build();
                                });
        }

        /**
         * List all schedules for a tenant.
         */
        @GET
        public Uni<Response> listSchedules(
                        @QueryParam("tenantId") String tenantId,
                        @QueryParam("workflowId") String workflowId,
                        @QueryParam("enabled") Boolean enabled) {

                // TODO: Implement filtering
                return Uni.createFrom().item(
                                Response.ok(List.of()).build());
        }

        /**
         * Get schedule by ID.
         */
        @GET
        @Path("/{scheduleId}")
        public Uni<Response> getSchedule(@PathParam("scheduleId") String scheduleId) {
                // TODO: Implement
                return Uni.createFrom().item(
                                Response.status(Response.Status.NOT_FOUND).build());
        }

        /**
         * Update schedule configuration.
         */
        @PUT
        @Path("/{scheduleId}")
        public Uni<Response> updateSchedule(
                        @PathParam("scheduleId") String scheduleId,
                        ScheduleUpdateRequest request) {

                return scheduler.updateSchedule(scheduleId, request)
                                .map(schedule -> Response.ok(toScheduleResponse(schedule)).build())
                                .onFailure().recoverWithItem(error -> Response.status(Response.Status.BAD_REQUEST)
                                                .entity(Map.of("error", error.getMessage()))
                                                .build());
        }

        /**
         * Delete a schedule.
         */
        @DELETE
        @Path("/{scheduleId}")
        public Uni<Response> deleteSchedule(@PathParam("scheduleId") String scheduleId) {
                return scheduler.deleteSchedule(scheduleId)
                                .map(v -> Response.noContent().build());
        }

        /**
         * Get schedule execution history.
         */
        @GET
        @Path("/{scheduleId}/history")
        public Uni<Response> getHistory(
                        @PathParam("scheduleId") String scheduleId,
                        @QueryParam("limit") @DefaultValue("50") int limit) {

                return scheduler.getExecutionHistory(scheduleId, limit)
                                .map(executions -> Response.ok(executions).build());
        }

        private ScheduleResponse toScheduleResponse(WorkflowSchedule schedule) {
                return new ScheduleResponse(
                                schedule.getScheduleId(),
                                schedule.getWorkflowId(),
                                schedule.isEnabled() ? "ENABLED" : "DISABLED",
                                schedule.getNextExecutionAt(),
                                schedule.getExecutionCount());
        }
}