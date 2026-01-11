package tech.kayys.wayang.mcp.api;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.mcp.domain.*;
import tech.kayys.wayang.mcp.dto.GenerateToolsRequest;
import tech.kayys.wayang.mcp.dto.ToolGenerationResult;
import tech.kayys.wayang.mcp.model.InvocationStatus;
import tech.kayys.wayang.mcp.parser.OpenApiToolGenerator;

import tech.kayys.wayang.mcp.runtime.ToolExecutionRequest;
import tech.kayys.wayang.mcp.runtime.ToolExecutionResult;
import tech.kayys.wayang.mcp.service.McpToolExecutor;

import java.util.*;

/**
 * ============================================================================
 * MCP SERVER REST API
 * ============================================================================
 */
@Path("/mcp")
@Tag(name = "MCP", description = "MCP Server API")
public class McpResource {

    private static final Logger LOG = LoggerFactory.getLogger(McpResource.class);

    @Inject
    OpenApiToolGenerator toolGenerator;

    @Inject
    McpToolExecutor toolExecutor;

    /**
     * Generate tools from OpenAPI specification
     */
    @POST
    @Path("/tools/generate")
    @Authenticated
    @Operation(summary = "Generate tools from OpenAPI spec")
    public Uni<RestResponse<ToolGenerationResult>> generateTools(
            @Valid GenerateToolsRequest request) {

        return toolGenerator.generateTools(request)
                .map(result -> RestResponse.ok(result))
                .onFailure().recoverWithItem(throwable -> {
                    // Log error
                    System.err.println("Tool generation failed: " + throwable.getMessage());

                    // Return error response
                    return RestResponse.ResponseBuilder.create(RestResponse.Status.INTERNAL_SERVER_ERROR,
                            new ToolGenerationResult(
                                    null,
                                    request.namespace(),
                                    0,
                                    List.of(),
                                    List.of("Generation failed: " + throwable.getMessage())))
                            .build();
                });
    }

    /**
     * Execute MCP tool
     */
    @POST
    @Path("/tools/execute")
    @Authenticated
    @Operation(summary = "Execute MCP tool")
    public Uni<RestResponse<ToolExecutionResult>> executeTool(
            @Valid ToolExecutionRequest request) {

        return toolExecutor.execute(request)
                .map(result -> RestResponse.ok(result))
                .onFailure().recoverWithItem(throwable -> {
                    LOG.error("Tool execution failed", throwable);
                    return RestResponse.ResponseBuilder.create(RestResponse.Status.INTERNAL_SERVER_ERROR,
                            ToolExecutionResult.failure(request.toolId(), InvocationStatus.FAILURE,
                                    "Execution failed: " + throwable.getMessage(), 0))
                            .build();
                });
    }

    /**
     * List available tools
     */
    @GET
    @Path("/tools")
    @Authenticated
    @Operation(summary = "List available tools")
    public Uni<RestResponse<List<McpTool>>> listTools(
            @QueryParam("namespace") String namespace,
            @QueryParam("capability") String capability) {

        // This would typically query the database for tools
        // For now, return empty list
        return Uni.createFrom().item(RestResponse.ok(List.of()));
    }

    /**
     * Get tool by ID
     */
    @GET
    @Path("/tools/{toolId}")
    @Authenticated
    @Operation(summary = "Get tool by ID")
    public Uni<RestResponse<McpTool>> getTool(@PathParam("toolId") String toolId) {

        // This would typically query the database for a specific tool
        // For now, return 404
        return Uni.createFrom().item(RestResponse.status(Response.Status.NOT_FOUND));
    }
}