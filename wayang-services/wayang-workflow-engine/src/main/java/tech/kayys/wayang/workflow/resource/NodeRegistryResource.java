package tech.kayys.wayang.workflow.resource;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import tech.kayys.wayang.workflow.service.NodeRegistry;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.workflow.api.dto.ErrorResponse;

import java.util.List;
import java.util.Map;

/**
 * NodeRegistryResource - REST API for node type management
 */
@Path("/api/v1/nodes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "Node Registry", description = "Node type and capability management")
public class NodeRegistryResource {

    private static final Logger LOG = Logger.getLogger(NodeRegistryResource.class);

    @Inject
    NodeRegistry nodeRegistry;

    @GET
    @Path("/types")
    @Operation(summary = "List node types", description = "List all available node types")
    public Uni<List<String>> listNodeTypes() {
        return nodeRegistry.getRegisteredNodeTypes();
    }

    @GET
    @Path("/types/{type}")
    @Operation(summary = "Get node type info", description = "Get information about a node type")
    public Uni<Response> getNodeTypeInfo(@PathParam("type") String type) {
        return nodeRegistry.getNodeTypeSchema(type)
                .map(schema -> {
                    if (schema == null) {
                        return Response.status(Response.Status.NOT_FOUND)
                                .entity(new ErrorResponse("NOT_FOUND", "Node type not found: " + type))
                                .build();
                    }
                    return Response.ok(schema).build();
                });
    }

    @POST
    @Path("/validate")
    @Operation(summary = "Validate node", description = "Validate node definition against its schema")
    public Uni<Response> validateNode(NodeDefinition node) {
        return nodeRegistry.validateNode(node)
                .map(isValid -> Response.ok(Map.of("valid", isValid)).build())
                .onFailure().recoverWithItem(th -> Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("VALIDATION_FAILED", th.getMessage()))
                        .build());
    }
}
