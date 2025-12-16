

// tech.kayys.platform.mcp.McpAutoExposer.java
package tech.kayys.service;

import tech.kayys.platform.executor.ComposableAgentExecutor;
import tech.kayys.platform.registry.AgentRegistry;
import tech.kayys.platform.schema.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Auto-exposes all tools in the registry as an MCP server.
 * Handles /tools and /call endpoints.
 */
public class McpAutoExposer {
    private final AgentRegistry registry;
    private final ComposableAgentExecutor executor;
    private final ObjectMapper mapper;

    public McpAutoExposer(
        AgentRegistry registry,
        ComposableAgentExecutor executor,
        ObjectMapper mapper
    ) {
        this.registry = registry;
        this.executor = executor;
        this.mapper = mapper;
    }

    /**
     * Handle GET /tools → returns MCP tool list
     */
    public String getTools() throws Exception {
        List<ToolDefinition> tools = registry.getAllTools(); // add this method to registry
        ArrayNode toolsArray = mapper.createArrayNode();

        for (ToolDefinition tool : tools) {
            ObjectNode toolNode = toolsArray.addObject();
            toolNode.put("name", tool.id());
            toolNode.put("description", tool.description().orElse(tool.name()));
            
            // Parameters as JSON Schema (MCP-compatible)
            ObjectNode paramsSchema = toolNode.putObject("parameters");
            paramsSchema.put("type", "object");
            ObjectNode props = paramsSchema.putObject("properties");
            ArrayNode required = paramsSchema.putArray("required");

            for (var param : tool.parameters()) {
                ObjectNode paramSchema = props.putObject(param.name());
                paramSchema.put("type", param.type());
                if (param.description().isPresent()) {
                    paramSchema.put("description", param.description().get());
                }
                if (param.required()) {
                    required.add(param.name());
                }
            }
        }

        ObjectNode response = mapper.createObjectNode();
        response.set("tools", toolsArray);
        return mapper.writeValueAsString(response);
    }

    /**
     * Handle POST /call → executes a tool
     */
    public String callTool(String requestBody) throws Exception {
        JsonNode request = mapper.readTree(requestBody);
        
        // Validate MCP call format
        if (!"call".equals(request.path("method").asText())) {
            throw new IllegalArgumentException("Only 'call' method supported");
        }

        JsonNode params = request.get("params");
        String toolName = params.path("name").asText();
        JsonNode arguments = params.path("arguments");

        // Convert arguments to Map<String, JsonNode>
        Map<String, JsonNode> input = mapper.convertValue(
            arguments,
            mapper.getTypeFactory().constructMapType(Map.class, String.class, JsonNode.class)
        );

        // Execute via your platform's executor
        // Note: We use a dummy agent that directly uses this tool
        JsonNode result = executor.executeCapability(
            createDummyAgentId(toolName),
            toolName,
            input
        );

        // MCP response: { "result": ... }
        ObjectNode response = mapper.createObjectNode();
        response.set("result", result);
        return mapper.writeValueAsString(response);
    }

    // Helper: Create a transient agent for direct tool execution
    private String createDummyAgentId(String toolId) {
        return "mcp-auto." + toolId;
    }
}