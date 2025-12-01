package tech.kayys.wayang.resource;


import java.util.Map;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.plugin.PluginMarketplace;

public class MarketplaceResource {
    
    @GET
    @Path("/plugins")
    public Response listPlugins(@QueryParam("search") String search) {
        var plugins = search != null && !search.isBlank() ?
            PluginMarketplace.searchPlugins(search) :
            PluginMarketplace.listPlugins();
        
        return Response.ok(Map.of(
            "plugins", plugins,
            "total", plugins.size()
        )).build();
    }
    
    @GET
    @Path("/plugins/{pluginId}")
    public Response getPlugin(@PathParam("pluginId") String pluginId) {
        var plugin = PluginMarketplace.getPlugin(pluginId);
        
        if (plugin == null) {
            return Response.status(404).entity("Plugin not found").build();
        }
        
        return Response.ok(plugin).build();
    }
    
    @POST
    @Path("/plugins/{pluginId}/install")
    public Response installPlugin(@PathParam("pluginId") String pluginId) {
        var plugin = PluginMarketplace.getPlugin(pluginId);
        
        if (plugin == null) {
            return Response.status(404).entity("Plugin not found").build();
        }
        
        // In real implementation, download and install the plugin
        return Response.ok(Map.of(
            "status", "installed",
            "plugin_id", pluginId,
            "message", "Plugin will be loaded on next restart"
        )).build();
    }
}
