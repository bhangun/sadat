package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.domain.WorkflowRun;

/**
 * Memory Optimization
 */
@ApplicationScoped
public class MemoryOptimizer {

    /**
     * Off-heap caching for large objects
     */
    public <T> T cacheOffHeap(String key, T value) {
        byte[] serialized = serialize(value);
        offHeapCache.put(key, serialized);
        return value;
    }

    /**
     * Streaming for large result sets
     */
    public Multi<WorkflowRun> streamLargeResultSet(String query) {
        return Multi.createFrom().publisher(
                entityManager.createQuery(query)
                        .setHint("javax.persistence.fetchSize", 100)
                        .getResultStream());
    }
}