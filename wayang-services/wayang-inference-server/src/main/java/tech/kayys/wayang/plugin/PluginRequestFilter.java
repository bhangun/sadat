package tech.kayys.wayang.plugin;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

public class PluginRequestFilter implements ContainerRequestFilter, ContainerResponseFilter {
    
    @Inject
    ServerPluginManager pluginManager;
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Call beforeRequest on all server plugins
        for (ServerPlugin plugin : pluginManager.getServerPlugins()) {
            plugin.beforeRequest(requestContext);
            
            // Check authorization
            if (!plugin.authorizeRequest(requestContext)) {
                requestContext.abortWith(
                    jakarta.ws.rs.core.Response.status(403)
                        .entity("Access denied by plugin: " + plugin.getName())
                        .build()
                );
                return;
            }
        }
    }
    
    @Override
    public void filter(ContainerRequestContext requestContext, 
                      ContainerResponseContext responseContext) throws IOException {
        // Call afterResponse on all server plugins
        for (ServerPlugin plugin : pluginManager.getServerPlugins()) {
            plugin.afterResponse(requestContext, responseContext);
        }
    }
}
