package tech.kayys.wayang.workflow.service.backup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for system configurations
 */
public class ConfigurationCache {
    private final Map<String, SystemConfig> cache = new ConcurrentHashMap<>();

    public void put(String key, SystemConfig config) {
        cache.put(key, config);
    }

    public SystemConfig get(String key) {
        return cache.get(key);
    }

    public void clear() {
        cache.clear();
    }
}
