package tech.kayys.wayang.plugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.ws.rs.container.ContainerRequestContext;
import tech.kayys.wayang.engine.EnginePlugin;

public class MultiTenancyPlugin implements ServerPlugin, EnginePlugin {
    private PluginContext context;
    private final Map<String, TenantConfig> tenants = new ConcurrentHashMap<>();
    private final ThreadLocal<String> currentTenant = new ThreadLocal<>();
    
    @Override
    public String getId() {
        return "multi-tenancy";
    }
    
    @Override
    public String getName() {
        return "Multi-Tenancy Plugin";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "Multi-tenant support with isolated resources and quotas";
    }
    
    @Override
    public void initialize(PluginContext context) throws PluginException {
        this.context = context;
        
        // Load tenant configurations
        loadTenantConfigs();
    }
    
    @Override
    public void start() {}
    
    @Override
    public void stop() {
        tenants.clear();
    }
    
    private void loadTenantConfigs() {
        // Example tenants
        tenants.put("tenant1", new TenantConfig(
            "tenant1",
            "Tenant One",
            1000,  // daily quota
            4096,  // max context size
            List.of("gpt-3.5", "gpt-4")
        ));
        
        tenants.put("tenant2", new TenantConfig(
            "tenant2",
            "Tenant Two",
            5000,
            8192,
            List.of("gpt-3.5")
        ));
    }
    
    @Override
    public void beforeRequest(ContainerRequestContext requestContext) {
        String tenantId = requestContext.getHeaderString("X-Tenant-ID");
        
        if (tenantId == null || !tenants.containsKey(tenantId)) {
            requestContext.abortWith(
                jakarta.ws.rs.core.Response.status(400)
                    .entity("Invalid or missing tenant ID")
                    .build()
            );
            return;
        }
        
        TenantConfig config = tenants.get(tenantId);
        
        // Check quota
        if (config.isQuotaExceeded()) {
            requestContext.abortWith(
                jakarta.ws.rs.core.Response.status(429)
                    .entity("Tenant quota exceeded")
                    .header("X-Quota-Limit", config.dailyQuota())
                    .header("X-Quota-Remaining", 0)
                    .build()
            );
            return;
        }
        
        currentTenant.set(tenantId);
        config.incrementUsage();
    }
    
    @Override
    public String preprocessPrompt(String prompt) {
        String tenantId = currentTenant.get();
        if (tenantId != null) {
            TenantConfig config = tenants.get(tenantId);
            if (config != null) {
                // Add tenant-specific prefix or context
                return String.format("[Tenant: %s] %s", config.name(), prompt);
            }
        }
        return prompt;
    }
    
    private static class TenantConfig {
        private final String id;
        private final String name;
        private final int dailyQuota;
        private final int maxContextSize;
        private final List<String> allowedModels;
        private int currentUsage = 0;
        
        TenantConfig(String id, String name, int dailyQuota, int maxContextSize, List<String> allowedModels) {
            this.id = id;
            this.name = name;
            this.dailyQuota = dailyQuota;
            this.maxContextSize = maxContextSize;
            this.allowedModels = allowedModels;
        }
        
        String id() {
            return id;
        }
        
        String name() {
            return name;
        }
        
        int dailyQuota() {
            return dailyQuota;
        }
        
        int maxContextSize() {
            return maxContextSize;
        }
        
        List<String> allowedModels() {
            return allowedModels;
        }
        
        boolean isQuotaExceeded() {
            return currentUsage >= dailyQuota;
        }
        
        void incrementUsage() {
            currentUsage++;
        }
        
        void resetUsage() {
            currentUsage = 0;
        }
    }
}
