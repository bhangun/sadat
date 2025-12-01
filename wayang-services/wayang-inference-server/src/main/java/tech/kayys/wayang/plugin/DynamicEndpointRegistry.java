package tech.kayys.wayang.plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.ws.rs.core.Response;

public class DynamicEndpointRegistry implements EndpointRegistry {
    
    private final Map<String, EndpointHandler> getHandlers = new ConcurrentHashMap<>();
    private final Map<String, EndpointHandler> postHandlers = new ConcurrentHashMap<>();
    private final Map<String, EndpointHandler> putHandlers = new ConcurrentHashMap<>();
    private final Map<String, EndpointHandler> deleteHandlers = new ConcurrentHashMap<>();
    
    @Override
    public void registerGet(String path, Function<RequestContext, Response> handler) {
        getHandlers.put(path, new EndpointHandler(path, "GET", handler));
    }
    
    @Override
    public void registerPost(String path, Function<RequestContext, Response> handler) {
        postHandlers.put(path, new EndpointHandler(path, "POST", handler));
    }
    
    @Override
    public void registerPut(String path, Function<RequestContext, Response> handler) {
        putHandlers.put(path, new EndpointHandler(path, "PUT", handler));
    }
    
    @Override
    public void registerDelete(String path, Function<RequestContext, Response> handler) {
        deleteHandlers.put(path, new EndpointHandler(path, "DELETE", handler));
    }
    
    public Response handle(String method, String path, RequestContext context) {
        Map<String, EndpointHandler> handlers = switch (method.toUpperCase()) {
            case "GET" -> getHandlers;
            case "POST" -> postHandlers;
            case "PUT" -> putHandlers;
            case "DELETE" -> deleteHandlers;
            default -> Map.of();
        };
        
        EndpointHandler handler = handlers.get(path);
        if (handler != null) {
            return handler.handler.apply(context);
        }
        
        return Response.status(404).entity("Endpoint not found").build();
    }
    
    public Map<String, EndpointHandler> getAllHandlers() {
        Map<String, EndpointHandler> all = new ConcurrentHashMap<>();
        all.putAll(getHandlers);
        all.putAll(postHandlers);
        all.putAll(putHandlers);
        all.putAll(deleteHandlers);
        return all;
    }
    
    public record EndpointHandler(
        String path,
        String method,
        Function<RequestContext, Response> handler
    ) {}

}
