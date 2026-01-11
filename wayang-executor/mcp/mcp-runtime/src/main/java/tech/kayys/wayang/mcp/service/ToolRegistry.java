package tech.kayys.wayang.mcp.service;

import java.util.List;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.mcp.domain.McpTool;
import tech.kayys.wayang.mcp.dto.ToolMetadata;
import tech.kayys.wayang.mcp.dto.ToolQuery;

import tech.kayys.wayang.mcp.runtime.ToolNotFoundException;

/**
 * Tool registry for resolving tools
 */
@ApplicationScoped
public class ToolRegistry {

    /**
     * Resolve tool by ID and tenant
     */
    public Uni<McpTool> resolveTool(String toolId, String tenantId) {
        return McpTool.<McpTool>find(
                "toolId = ?1 and tenantId = ?2 and enabled = true",
                toolId, tenantId).firstResult()
                .onItem().ifNull().failWith(() -> new ToolNotFoundException("Tool not found: " + toolId));
    }

    /**
     * List tools for tenant with filters
     */
    public Uni<List<ToolMetadata>> listTools(ToolQuery query) {
        // Implementation for tool discovery
        return Uni.createFrom().item(List.of());
    }
}
