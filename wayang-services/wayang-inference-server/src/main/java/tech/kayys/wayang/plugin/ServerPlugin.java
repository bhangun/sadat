package tech.kayys.wayang.plugin;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;

public interface ServerPlugin extends Plugin {
    
    /**
     * Called before a request is processed
     */
    default void beforeRequest(ContainerRequestContext requestContext) {}
    
    /**
     * Called after a response is generated
     */
    default void afterResponse(ContainerRequestContext requestContext,
                               ContainerResponseContext responseContext) {}
    
    /**
     * Register custom REST endpoints
     */
    default void registerEndpoints(EndpointRegistry registry) {}
    
    /**
     * Modify response before sending
     */
    default Object transformResponse(Object response, String endpoint) {
        return response;
    }
    
    /**
     * Authenticate/authorize requests
     */
    default boolean authorizeRequest(ContainerRequestContext requestContext) {
        return true;
    }
}
