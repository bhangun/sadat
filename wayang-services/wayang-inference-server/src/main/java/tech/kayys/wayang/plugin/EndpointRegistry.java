package tech.kayys.wayang.plugin;

import java.util.function.Function;

import jakarta.ws.rs.core.Response;

public interface EndpointRegistry {
    
    /**
     * Register a GET endpoint
     */
    void registerGet(String path, Function<RequestContext, Response> handler);
    
    /**
     * Register a POST endpoint
     */
    void registerPost(String path, Function<RequestContext, Response> handler);
    
    /**
     * Register a PUT endpoint
     */
    void registerPut(String path, Function<RequestContext, Response> handler);
    
    /**
     * Register a DELETE endpoint
     */
    void registerDelete(String path, Function<RequestContext, Response> handler);
    
    interface RequestContext {
        String getPath();
        String getMethod();
        Object getBody();
        String getHeader(String name);
        String getQueryParam(String name);
        <T> T getBodyAs(Class<T> type);
    }
}
