package tech.kayys.wayang.workflow.backup.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import tech.kayys.wayang.workflow.model.WorkflowSnapshot;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Cache for workflow snapshots
 */
@ApplicationScoped
public class SnapshotCache {
    private final Map<String, WorkflowSnapshot> cache = new ConcurrentHashMap<>();

    public void put(String key, WorkflowSnapshot snapshot) {
        cache.put(key, snapshot);
    }

    public WorkflowSnapshot get(String key) {
        return cache.get(key);
    }

    public void remove(String key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }
}
