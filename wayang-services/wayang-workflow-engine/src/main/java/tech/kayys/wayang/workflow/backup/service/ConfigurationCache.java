package tech.kayys.wayang.workflow.backup.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.backup.domain.SystemConfig;

/**
 * Cache for system configurations
 */
@ApplicationScoped
public class ConfigurationCache {
    private final Map<String, SystemConfig> cache = new ConcurrentHashMap<>();

    public void put(String key, SystemConfig config) {
        cache.put(key, config);
    }

    public SystemConfig get(String key) {
        return cache.get(key);
    }

    public void invalidate(String configKey, String tenantId) {
        String cacheKey = configKey + ":" + tenantId;
        cache.remove(cacheKey);
    }

    public void clear() {
        cache.clear();
    }
}
