package tech.kayys.wayang.service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.tools.Tool;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import tech.kayys.wayang.mcp.MCPRegistry;
import tech.kayys.wayang.plugin.FunctionRegistry;

public class MCPIntegrationService {
    private static final Logger log = Logger.getLogger(MCPIntegrationService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    @ConfigProperty(name = "llama.mcp.config-path", defaultValue = "./config/mcp-config.json")
    String mcpConfigPath;
    
    @ConfigProperty(name = "llama.mcp.enabled", defaultValue = "true")
    boolean mcpEnabled;
    
    @Inject
    FunctionRegistry functionRegistry;
    
    private MCPRegistry mcpRegistry;
    
    @PostConstruct
    void initialize() {
        if (!mcpEnabled) {
            log.info("MCP integration is disabled");
            return;
        }
        
        try {
            log.info("Initializing MCP integration...");
            
            Path configPath = Paths.get(mcpConfigPath);
            mcpRegistry = new MCPRegistry(configPath);
            mcpRegistry.loadFromConfig();
            
            // Register all MCP tools with the function registry
            registerMCPTools();
            
            log.info("MCP integration initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize MCP integration", e);
        }
    }
    
    private void registerMCPTools() {
        Map<String, java.util.List<MCPTool>> allTools = mcpRegistry.getAllTools();
        
        for (Map.Entry<String, java.util.List<MCPTool>> entry : allTools.entrySet()) {
            String serverName = entry.getKey();
            
            for (MCPTool mcpTool : entry.getValue()) {
                try {
                    // Convert MCP tool to platform tool
                    Tool tool = convertMCPToolToTool(serverName, mcpTool);
                    
                    // Register with function registry
                    functionRegistry.register(tool, arguments -> {
                        try {
                            return mcpRegistry.executeTool(serverName, mcpTool.name(), arguments);
                        } catch (MCPException e) {
                            log.error("MCP tool execution failed", e);
                            return "Error: " + e.getMessage();
                        }
                    });
                    
                    log.infof("Registered MCP tool: %s/%s", serverName, mcpTool.name());
                    
                } catch (Exception e) {
                    log.errorf(e, "Failed to register MCP tool: %s/%s", serverName, mcpTool.name());
                }
            }
        }
    }
    
    private Tool convertMCPToolToTool(String serverName, MCPTool mcpTool) {
        try {
            Map<String, Object> schema = mapper.readValue(
                mcpTool.getSchemaString(), 
                Map.class
            );
            
            return Tool.function(
                serverName + "." + mcpTool.name(),
                mcpTool.description(),
                schema
            );
        } catch (Exception e) {
            return Tool.function(
                serverName + "." + mcpTool.name(),
                mcpTool.description(),
                Map.of("type", "object")
            );
        }
    }
    
    public MCPRegistry getMCPRegistry() {
        return mcpRegistry;
    }
    
    @PreDestroy
    void cleanup() {
        if (mcpRegistry != null) {
            mcpRegistry.close();
        }
    }
}
