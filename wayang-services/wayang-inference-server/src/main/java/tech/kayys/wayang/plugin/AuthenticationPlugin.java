package tech.kayys.wayang.plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;

public class AuthenticationPlugin implements ServerPlugin {
    private PluginContext context;
    private final Map<String, ApiKey> apiKeys = new ConcurrentHashMap<>();
    
    @Override
    public String getId() {
        return "auth-plugin";
    }
    
    @Override
    public String getName() {
        return "Authentication Plugin";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "API key authentication";
    }
    
    @Override
    public void initialize(PluginContext context) throws PluginException {
        this.context = context;
        
        // Load API keys from config
        String masterKey = context.getConfigValue("auth.master_key", String.class);
        if (masterKey != null) {
            apiKeys.put(masterKey, new ApiKey(masterKey, "master", Integer.MAX_VALUE));
        }
    }
    
    @Override
    public void start() {}
    
    @Override
    public void stop() {
        apiKeys.clear();
    }
    
    @Override
    public void registerEndpoints(EndpointRegistry registry) {
        // Register API key management endpoints
        registry.registerPost("/plugins/auth/keys", ctx -> {
            String name = ctx.getQueryParam("name");
            String key = generateApiKey();
            apiKeys.put(key, new ApiKey(key, name, 1000));
            
            return Response.ok(Map.of("api_key", key, "name", name)).build();
        });
        
        registry.registerGet("/plugins/auth/keys", ctx -> {
            return Response.ok(Map.of("keys", apiKeys.values())).build();
        });
        
        registry.registerDelete("/plugins/auth/keys", ctx -> {
            String key = ctx.getQueryParam("key");
            apiKeys.remove(key);
            return Response.ok(Map.of("deleted", true)).build();
        });
    }
    
    @Override
    public boolean authorizeRequest(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        
        // Skip auth for health checks
        if (path.contains("/health") || path.contains("/metrics")) {
            return true;
        }
        
        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return false;
        }
        
        String key = authHeader.substring(7);
        ApiKey apiKey = apiKeys.get(key);
        
        if (apiKey == null) {
            return false;
        }
        
        // Check rate limit
        if (apiKey.requestCount >= apiKey.rateLimit) {
            return false;
        }
        
        apiKey.requestCount++;
        return true;
    }
    
    private String generateApiKey() {
        return "sk-" + java.util.UUID.randomUUID().toString().replace("-", "");
    }
    
    private static class ApiKey {
        final String key;
        final String name;
        final int rateLimit;
        int requestCount = 0;
        
        ApiKey(String key, String name, int rateLimit) {
            this.key = key;
            this.name = name;
            this.rateLimit = rateLimit;
        }
    }
}
