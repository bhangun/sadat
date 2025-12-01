package tech.kayys.wayang.tools.mcp;

import tech.kayys.wayang.common.domain.*;
import io.smallrye.mutiny.Uni;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Model Context Protocol (MCP) Tool Interface
 * All tools must implement this contract
 */
public interface MCPTool {
    
    /**
     * Tool metadata
     */
    ToolDescriptor descriptor();
    
    /**
     * JSON Schema for input validation
     */
    JsonNode schema();
    
    /**
     * Execute tool with given request
     */
    Uni<ToolResponse> execute(ToolRequest request);
    
    /**
     * Required secret scopes (e.g., ["db/readonly", "api/external"])
     */
    List<String> requiredSecrets();
    
    /**
     * Tool capabilities
     */
    Set<ToolCapability> capabilities();
}