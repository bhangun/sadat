package tech.kayys.wayang.plugin;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public class DynamicPluginResource {
    
    @Inject
    ServerPluginManager pluginManager;
    
    @Context
    UriInfo uriInfo;
    
    @Context
    HttpHeaders headers;
    
    @GET
    @Path("/{pluginId}/{path: .*}")
    public Response handleGet(@PathParam("pluginId") String pluginId,
                             @PathParam("path") String path) {
        return handleRequest("GET", pluginId, path, null);
    }
    
    @POST
    @Path("/{pluginId}/{path: .*}")
    public Response handlePost(@PathParam("pluginId") String pluginId,
                              @PathParam("path") String path,
                              String body) {
        return handleRequest("POST", pluginId, path, body);
    }
    
    @PUT
    @Path("/{pluginId}/{path: .*}")
    public Response handlePut(@PathParam("pluginId") String pluginId,
                             @PathParam("path") String path,
                             String body) {
        return handleRequest("PUT", pluginId, path, body);
    }
    
    @DELETE
    @Path("/{pluginId}/{path: .*}")
    public Response handleDelete(@PathParam("pluginId") String pluginId,
                                @PathParam("path") String path) {
        return handleRequest("DELETE", pluginId, path, null);
    }
    
    private Response handleRequest(String method, String pluginId, String path, String body) {
        String fullPath = "/plugins/" + pluginId + "/" + path;
        
        EndpointRegistry.RequestContext context = new EndpointRegistry.RequestContext() {
            @Override
            public String getPath() {
                return fullPath;
            }
            
            @Override
            public String getMethod() {
                return method;
            }
            
            @Override
            public Object getBody() {
                return body;
            }
            
            @Override
            public String getHeader(String name) {
                return headers.getHeaderString(name);
            }
            
            @Override
            public String getQueryParam(String name) {
                return uriInfo.getQueryParameters().getFirst(name);
            }
            
            @Override
            public <T> T getBodyAs(Class<T> type) {
                // Simple JSON parsing (in real implementation, use Jackson)
                return null;
            }
        };
        
        return pluginManager.getEndpointRegistry().handle(method, fullPath, context);
    }
}
