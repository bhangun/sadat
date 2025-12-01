package tech.kayys.wayang.resource;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.mcp.MCPException;
import tech.kayys.wayang.mcp.MCPRegistry;
import tech.kayys.wayang.mcp.MCPTool;
import tech.kayys.wayang.service.MCPIntegrationService;

public class MCPResource {
    
    @Inject
    MCPIntegrationService mcpService;
    
    @GET
    @Path("/servers")
    public Response listServers() {
        MCPRegistry registry = mcpService.getMCPRegistry();
        if (registry == null) {
            return Response.status(503)
                .entity(Map.of("error", "MCP not initialized"))
                .build();
        }
        
        Map<String, MCPRegistry.MCPServerInfo> servers = registry.getServers();
        return Response.ok(Map.of("servers", servers)).build();
    }
    
    @GET
    @Path("/servers/{serverName}")
    public Response getServer(@PathParam("serverName") String serverName) {
        MCPRegistry registry = mcpService.getMCPRegistry();
        if (registry == null) {
            return Response.status(503)
                .entity(Map.of("error", "MCP not initialized"))
                .build();
        }
        
        Map<String, MCPRegistry.MCPServerInfo> servers = registry.getServers();
        MCPRegistry.MCPServerInfo server = servers.get(serverName);
        
        if (server == null) {
            return Response.status(404)
                .entity(Map.of("error", "Server not found"))
                .build();
        }
        
        return Response.ok(server).build();
    }
    
    @GET
    @Path("/servers/{serverName}/tools")
    public Response getServerTools(@PathParam("serverName") String serverName) {
        MCPRegistry registry = mcpService.getMCPRegistry();
        if (registry == null) {
            return Response.status(503)
                .entity(Map.of("error", "MCP not initialized"))
                .build();
        }
        
        var tools = registry.getTools(serverName);
        return Response.ok(Map.of("tools", tools)).build();
    }
    
    @GET
    @Path("/tools")
    public Response listAllTools() {
        MCPRegistry registry = mcpService.getMCPRegistry();
        if (registry == null) {
            return Response.status(503)
                .entity(Map.of("error", "MCP not initialized"))
                .build();
        }
        
        Map<String, java.util.List<MCPTool>> allTools = registry.getAllTools();
        return Response.ok(Map.of("tools", allTools)).build();
    }
    
    @POST
    @Path("/servers/{serverName}/restart")
    public Response restartServer(@PathParam("serverName") String serverName) {
        MCPRegistry registry = mcpService.getMCPRegistry();
        if (registry == null) {
            return Response.status(503)
                .entity(Map.of("error", "MCP not initialized"))
                .build();
        }
        
        try {
            registry.restartServer(serverName);
            return Response.ok(Map.of(
                "status", "restarted",
                "server", serverName
            )).build();
        } catch (MCPException e) {
            return Response.status(500)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
}
