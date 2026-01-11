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

import tech.kayys.wayang.mcp.SpecFormatRegistry;
import tech.kayys.wayang.mcp.domain.*;
import tech.kayys.wayang.mcp.dto.GenerateToolsRequest;
import tech.kayys.wayang.mcp.dto.OpenApiToolRequest;
import tech.kayys.wayang.mcp.dto.TenantContext;
import tech.kayys.wayang.mcp.dto.ToolDetailResponse;
import tech.kayys.wayang.mcp.dto.ToolExecuteRequest;
import tech.kayys.wayang.mcp.dto.ToolExecutionResponse;
import tech.kayys.wayang.mcp.dto.ToolGenerationResponse;
import tech.kayys.wayang.mcp.dto.ToolMetadataResponse;
import tech.kayys.wayang.mcp.dto.ToolUpdateRequest;
import tech.kayys.wayang.mcp.model.InvocationStatus;
import tech.kayys.wayang.mcp.parser.OpenApiToolGenerator;
import tech.kayys.wayang.mcp.repository.McpToolRepository;
import tech.kayys.wayang.mcp.runtime.ToolExecutionRequest;
import tech.kayys.wayang.mcp.service.McpToolExecutor;

import java.util.*;

/**
 * ============================================================================
 * MCP SERVER REST API
 * ============================================================================
 *
 * RESTful API for MCP tool management and execution.
 *
 * Endpoints:
 * - POST /api/v1/mcp/tools/openapi - Generate tools from OpenAPI
 * - GET /api/v1/mcp/tools - List tools
 * - GET /api/v1/mcp/tools/{toolId} - Get tool details
 * - POST /api/v1/mcp/tools/{toolId}/execute - Execute tool
 * - POST /api/v1/mcp/auth-profiles - Create auth profile
 */

// ==================== TOOL GENERATION API ====================

@Path("/api/v1/mcp/tools")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@Tag(name = "MCP Tools", description = "MCP tool management")
public class McpToolResource {

    @Inject
    OpenApiToolGenerator toolGenerator;

    @Inject
    McpToolExecutor toolExecutor;

    @Inject
    TenantContext tenantContext;

    @Inject
    SpecFormatRegistry specFormatRegistry;

    @Inject
    McpToolRepository mcpToolRepository;

    /**
     * Get supported specification formats
     */
    @GET
    @Path("/formats")
    @Operation(summary = "List supported API specification formats")
    public Map<String, SpecFormatRegistry.SpecFormatInfo> getSupportedFormats() {
        return specFormatRegistry.getSupportedFormats();
    }

    /**
     * Generate tools from OpenAPI specification
     */
    @POST
    @Path("/openapi")
    @Operation(summary = "Generate MCP tools from OpenAPI spec")
    public Uni<RestResponse<ToolGenerationResponse>> generateFromOpenApi(
            @Valid OpenApiToolRequest request) {

        String tenantId = tenantContext.getCurrentTenantId();

        GenerateToolsRequest genRequest = new GenerateToolsRequest(
                tenantId,
                request.namespace(),
                request.sourceType(),
                request.source(),
                request.authProfileId(),
                "current-user", // From security context
                request.guardrailsConfig() != null ? request.guardrailsConfig() : Map.of());

        return toolGenerator.generateTools(genRequest)
                .map(result -> RestResponse.ok(
                        new ToolGenerationResponse(
                                result.sourceId().toString(),
                                result.namespace(),
                                result.toolsGenerated(),
                                result.toolIds(),
                                result.warnings())))
                .onFailure().recoverWithItem(error -> RestResponse.status(
                        RestResponse.Status.BAD_REQUEST,
                        new ToolGenerationResponse(
                                null,
                                request.namespace(),
                                0,
                                List.of(),
                                List.of(error.getMessage()))));
    }

    /**
     * List tools for tenant
     */
    @GET
    @Operation(summary = "List MCP tools")
    public Uni<List<ToolMetadataResponse>> listTools(
            @QueryParam("namespace") String namespace,
            @QueryParam("capability") String capability,
            @QueryParam("tag") String tag,
            @QueryParam("enabled") Boolean enabled,
            @QueryParam("readOnly") Boolean readOnly) {

        String tenantId = tenantContext.getCurrentTenantId();

        String query = "tenantId = ?1";
        List<Object> params = new ArrayList<>();
        params.add(tenantId);

        if (namespace != null) {
            query += " and namespace = ?" + (params.size() + 1);
            params.add(namespace);
        }
        if (enabled != null) {
            query += " and enabled = ?" + (params.size() + 1);
            params.add(enabled);
        }
        if (readOnly != null) {
            query += " and readOnly = ?" + (params.size() + 1);
            params.add(readOnly);
        }

        return mcpToolRepository.searchTools(query, params.toArray())
                .map(tools -> tools.stream()
                        .map(tool -> new ToolMetadataResponse(
                                tool.getToolId(),
                                tool.getName(),
                                tool.getDescription(),
                                tool.getCapabilities(),
                                tool.getCapabilityLevel().name(),
                                tool.isReadOnly(),
                                tool.getTags()))
                        .toList());
    }

    /**
     * Get tool details
     */
    @GET
    @Path("/{toolId}")
    @Operation(summary = "Get tool details")
    public Uni<RestResponse<ToolDetailResponse>> getTool(
            @PathParam("toolId") String toolId) {

        String tenantId = tenantContext.getCurrentTenantId();

        return mcpToolRepository.findByTenantIdAndToolId(tenantId, toolId)
                .map(tool -> {
                    if (tool == null) {
                        return RestResponse.notFound();
                    }

                    return RestResponse.ok(new ToolDetailResponse(
                            tool.getToolId(),
                            tool.getName(),
                            tool.getDescription(),
                            tool.getInputSchema(),
                            tool.getOutputSchema(),
                            tool.getCapabilities(),
                            tool.getCapabilityLevel().name(),
                            tool.isEnabled(),
                            tool.isReadOnly(),
                            tool.getMetrics() != null ? tool.getMetrics().getTotalInvocations() : 0L));
                });
    }

    /**
     * Execute tool
     */
    @POST
    @Path("/{toolId}/execute")
    @Operation(summary = "Execute MCP tool")
    public Uni<RestResponse<ToolExecutionResponse>> executeTool(
            @PathParam("toolId") String toolId,
            @Valid ToolExecuteRequest request) {

        String tenantId = tenantContext.getCurrentTenantId();

        ToolExecutionRequest execRequest = new ToolExecutionRequest(
                tenantId,
                "current-user", // userId
                toolId,
                request.arguments(),
                null, // workflow run ID
                null, // agent ID
                request.context() != null ? request.context() : Map.of());

        return toolExecutor.execute(execRequest)
                .map(result -> {
                    if (result.status() == InvocationStatus.SUCCESS) {
                        return RestResponse.ok(new ToolExecutionResponse(
                                "success",
                                result.output(),
                                null,
                                result.executionTimeMs()));
                    } else {
                        return RestResponse.status(
                                RestResponse.Status.BAD_REQUEST,
                                new ToolExecutionResponse(
                                        "failure",
                                        Map.of(),
                                        result.errorMessage(),
                                        result.executionTimeMs()));
                    }
                });
    }

    /**
     * Update tool configuration
     */
    @PUT
    @Path("/{toolId}")
    @Operation(summary = "Update tool configuration")
    public Uni<RestResponse<Void>> updateTool(
            @PathParam("toolId") String toolId,
            @Valid ToolUpdateRequest request) {

        String tenantId = tenantContext.getCurrentTenantId();

        return mcpToolRepository.findByTenantIdAndToolId(tenantId, toolId)
                .flatMap(tool -> {
                    if (tool == null) {
                        return Uni.createFrom().item(RestResponse.notFound());
                    }

                    if (request.enabled() != null) {
                        tool.setEnabled(request.enabled());
                    }
                    if (request.description() != null) {
                        tool.setDescription(request.description());
                    }
                    if (request.tags() != null) {
                        tool.setTags(request.tags());
                    }

                    return mcpToolRepository.update(tool)
                            .map(v -> RestResponse.ok());
                });
    }

    /**
     * Delete tool
     */
    @DELETE
    @Path("/{toolId}")
    @Operation(summary = "Delete tool")
    public Uni<RestResponse<Void>> deleteTool(
            @PathParam("toolId") String toolId) {

        String tenantId = tenantContext.getCurrentTenantId();

        return mcpToolRepository.findByTenantIdAndToolId(tenantId, toolId)
                .flatMap(tool -> {
                    if (tool == null) {
                        return Uni.createFrom().item(RestResponse.notFound());
                    }

                    return mcpToolRepository.deleteById(toolId)
                            .map(deleted -> deleted ? RestResponse.ok() : RestResponse.notFound());
                });
    }
}