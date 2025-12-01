package tech.kayys.wayang.plugin;

import java.nio.file.Path;
import java.util.List;

import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import tech.kayys.wayang.engine.LlamaEngine;

public class ServerPluginManager {
    private static final Logger log = Logger.getLogger(ServerPluginManager.class);
    
    private final PluginManager corePluginManager;
    private final DynamicEndpointRegistry endpointRegistry;
    
    @Inject
    public ServerPluginManager(LlamaEngine engine) {
        Path pluginsDir = Path.of("./plugins");
        Path dataDir = Path.of("./plugin-data");
        
        this.corePluginManager = engine.getPluginManager();
        this.endpointRegistry = new DynamicEndpointRegistry();
        
        // Register server-specific endpoints
        registerServerPluginEndpoints();
    }
    
    private void registerServerPluginEndpoints() {
        List<ServerPlugin> serverPlugins = corePluginManager.getPluginsByType(ServerPlugin.class);
        
        for (ServerPlugin plugin : serverPlugins) {
            try {
                plugin.registerEndpoints(endpointRegistry);
                log.infof("Registered endpoints for plugin: %s", plugin.getName());
            } catch (Exception e) {
                log.errorf(e, "Failed to register endpoints for plugin: %s", plugin.getId());
            }
        }
    }
    
    public List<ServerPlugin> getServerPlugins() {
        return corePluginManager.getPluginsByType(ServerPlugin.class);
    }
    
    public DynamicEndpointRegistry getEndpointRegistry() {
        return endpointRegistry;
    }
}
