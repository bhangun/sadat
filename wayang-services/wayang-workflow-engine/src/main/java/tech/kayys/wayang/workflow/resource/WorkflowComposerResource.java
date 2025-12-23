package tech.kayys.wayang.workflow.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import tech.kayys.wayang.workflow.service.WorkflowComposer;
import tech.kayys.wayang.schema.workflow.WorkflowDefinition;
import tech.kayys.wayang.workflow.api.dto.ErrorResponse;

import java.util.List;

/**
 * WorkflowComposerResource - REST API for workflow composition
 * 
 * Endpoints:
 * - POST /api/v1/composer/compose - Compose workflows
 * - POST /api/v1/composer/fork-join - Create fork-join workflow
 * - POST /api/v1/composer/sequential - Create sequential workflow
 * - POST /api/v1/composer/conditional - Create conditional workflow
 */
@Path("/api/v1/composer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Workflow Composer", description = "Workflow composition services")
public class WorkflowComposerResource {

        private static final Logger LOG = Logger.getLogger(WorkflowComposerResource.class);

        @Inject
        WorkflowComposer composer;

        /**
         * Compose workflows
         */
        @POST
        @Path("/compose")
        @Operation(summary = "Compose workflows", description = "Create a parent workflow composed of child workflows")
        public Uni<Response> composeWorkflows(
                        @QueryParam("parentId") String parentId,
                        List<String> childIds) {

                if (parentId == null || childIds == null || childIds.isEmpty()) {
                        return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                                        .entity(new ErrorResponse("INVALID_REQUEST",
                                                        "Parent ID and child IDs are required"))
                                        .build());
                }

                return composer.compose(parentId, childIds)
                                .map(w -> Response.status(Response.Status.CREATED).entity(w).build())
                                .onFailure().recoverWithItem(th -> Response.status(Response.Status.BAD_REQUEST)
                                                .entity(new ErrorResponse("COMPOSITION_FAILED", th.getMessage()))
                                                .build());
        }

        /**
         * Create fork-join workflow
         */
        @POST
        @Path("/fork-join")
        @Operation(summary = "Create fork-join", description = "Create a workflow with fork-join pattern")
        public Uni<Response> createForkJoin(
                        @QueryParam("id") String id,
                        List<String> branchIds) {

                if (id == null || branchIds == null || branchIds.isEmpty()) {
                        return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                                        .entity(new ErrorResponse("INVALID_REQUEST", "ID and branch IDs are required"))
                                        .build());
                }

                return Uni.createFrom().item(composer.forkJoin(id, branchIds))
                                .map(w -> Response.status(Response.Status.CREATED).entity(w).build())
                                .onFailure().recoverWithItem(th -> Response.status(Response.Status.BAD_REQUEST)
                                                .entity(new ErrorResponse("COMPOSITION_FAILED", th.getMessage()))
                                                .build());
        }

        /**
         * Create sequential workflow
         */
        @POST
        @Path("/sequential")
        @Operation(summary = "Create sequential", description = "Create a workflow execution sequence")
        public Uni<Response> createSequential(
                        @QueryParam("id") String id,
                        List<String> stepIds) {

                if (id == null || stepIds == null || stepIds.isEmpty()) {
                        return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                                        .entity(new ErrorResponse("INVALID_REQUEST", "ID and step IDs are required"))
                                        .build());
                }

                return composer.sequential(id, stepIds)
                                .map(w -> Response.status(Response.Status.CREATED).entity(w).build())
                                .onFailure().recoverWithItem(th -> Response.status(Response.Status.BAD_REQUEST)
                                                .entity(new ErrorResponse("COMPOSITION_FAILED", th.getMessage()))
                                                .build());
        }

        /**
         * Create conditional workflow
         */
        @POST
        @Path("/conditional")
        @Operation(summary = "Create conditional", description = "Create a workflow with conditional logic")
        public Uni<Response> createConditional(
                        @QueryParam("id") String id,
                        List<String> nodeIds) {

                if (id == null || nodeIds == null || nodeIds.isEmpty()) {
                        return Uni.createFrom().item(Response.status(Response.Status.BAD_REQUEST)
                                        .entity(new ErrorResponse("INVALID_REQUEST", "ID and node IDs are required"))
                                        .build());
                }

                return Uni.createFrom().item(composer.conditional(id, nodeIds))
                                .map(w -> Response.status(Response.Status.CREATED).entity(w).build())
                                .onFailure().recoverWithItem(th -> Response.status(Response.Status.BAD_REQUEST)
                                                .entity(new ErrorResponse("COMPOSITION_FAILED", th.getMessage()))
                                                .build());
        }
}
