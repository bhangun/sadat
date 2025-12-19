package tech.kayys.wayang.workflow.service.backup;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for workflow snapshots
 */
public class SnapshotCache {
    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    public void put(String key, Object snapshot) {
        cache.put(key, snapshot);
    }

    public Object get(String key) {
        return cache.get(key);
    }

    public void clear() {
        cache.clear();
    }
}
