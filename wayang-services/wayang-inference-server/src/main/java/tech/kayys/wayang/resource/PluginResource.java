package tech.kayys.wayang.resource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.plugin.Plugin;
import tech.kayys.wayang.plugin.ServerPluginManager;

public class PluginResource {
    
    @Inject
    ServerPluginManager pluginManager;
    
    @GET
    public Response listPlugins() {
        Map<String, Plugin> plugins = pluginManager.getServerPlugins().stream()
            .collect(Collectors.toMap(Plugin::getId, p -> p));
        
        List<PluginInfo> infos = plugins.values().stream()
            .map(this::toPluginInfo)
            .toList();
        
        return Response.ok(Map.of("plugins", infos)).build();
    }
    
    @GET
    @Path("/{pluginId}")
    public Response getPlugin(@PathParam("pluginId") String pluginId) {
        Plugin plugin = pluginManager.getServerPlugins().stream()
            .filter(p -> p.getId().equals(pluginId))
            .findFirst()
            .orElse(null);
        
        if (plugin == null) {
            return Response.status(404).entity("Plugin not found").build();
        }
        
        return Response.ok(toPluginInfo(plugin)).build();
    }
    
    @GET
    @Path("/{pluginId}/health")
    public Response checkHealth(@PathParam("pluginId") String pluginId) {
        Plugin plugin = pluginManager.getServerPlugins().stream()
            .filter(p -> p.getId().equals(pluginId))
            .findFirst()
            .orElse(null);
        
        if (plugin == null) {
            return Response.status(404).entity("Plugin not found").build();
        }
        
        boolean healthy = plugin.isHealthy();
        return Response.ok(Map.of(
            "plugin_id", pluginId,
            "healthy", healthy,
            "status", healthy ? "UP" : "DOWN"
        )).build();
    }
    
    private PluginInfo toPluginInfo(Plugin plugin) {
        return new PluginInfo(
            plugin.getId(),
            plugin.getName(),
            plugin.getVersion(),
            plugin.getDescription(),
            plugin.getAuthor(),
            plugin.isHealthy(),
            List.of(plugin.getDependencies())
        );
    }
    
    private record PluginInfo(
        String id,
        String name,
        String version,
        String description,
        String author,
        boolean healthy,
        List<String> dependencies
    ) {}
}
