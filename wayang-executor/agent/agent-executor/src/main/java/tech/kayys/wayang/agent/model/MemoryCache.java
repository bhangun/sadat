package tech.kayys.wayang.agent.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * In-memory cache for active conversation sessions
 */
@ApplicationScoped
public class MemoryCache {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryCache.class);
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_TTL_MS = 3600000; // 1 hour

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public Uni<List<Message>> get(String sessionId, String tenantId) {
        String key = makeKey(sessionId, tenantId);
        CacheEntry entry = cache.get(key);

        if (entry != null && !entry.isExpired()) {
            entry.updateAccessTime();
            LOG.trace("Cache hit: {}", key);
            return Uni.createFrom().item(new ArrayList<>(entry.messages));
        }

        LOG.trace("Cache miss: {}", key);
        return Uni.createFrom().nullItem();
    }

    public void put(String sessionId, String tenantId, List<Message> messages) {
        String key = makeKey(sessionId, tenantId);

        // Evict old entries if cache is full
        if (cache.size() >= MAX_CACHE_SIZE) {
            evictOldest();
        }

        cache.put(key, new CacheEntry(new ArrayList<>(messages)));
        LOG.trace("Cache put: {} ({} messages)", key, messages.size());
    }

    public void append(String sessionId, String tenantId, List<Message> messages) {
        String key = makeKey(sessionId, tenantId);
        CacheEntry entry = cache.get(key);

        if (entry != null && !entry.isExpired()) {
            entry.messages.addAll(messages);
            entry.updateAccessTime();
            LOG.trace("Cache append: {} (+{} messages)", key, messages.size());
        } else {
            put(sessionId, tenantId, messages);
        }
    }

    public void invalidate(String sessionId, String tenantId) {
        String key = makeKey(sessionId, tenantId);
        cache.remove(key);
        LOG.trace("Cache invalidated: {}", key);
    }

    public boolean isCached(String sessionId, String tenantId) {
        String key = makeKey(sessionId, tenantId);
        CacheEntry entry = cache.get(key);
        return entry != null && !entry.isExpired();
    }

    private String makeKey(String sessionId, String tenantId) {
        return tenantId + ":" + sessionId;
    }

    private void evictOldest() {
        cache.entrySet().stream()
                .min(Comparator.comparing(e -> e.getValue().lastAccessTime))
                .ifPresent(entry -> {
                    cache.remove(entry.getKey());
                    LOG.debug("Evicted cache entry: {}", entry.getKey());
                });
    }

    private static class CacheEntry {
        final List<Message> messages;
        long lastAccessTime;
        final long creationTime;

        CacheEntry(List<Message> messages) {
            this.messages = messages;
            this.creationTime = System.currentTimeMillis();
            this.lastAccessTime = creationTime;
        }

        void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - creationTime > CACHE_TTL_MS;
        }
    }
}
