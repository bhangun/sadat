package tech.kayys.wayang.workflow.service;

import io.quarkus.cache.Cache;
import io.quarkus.cache.CacheName;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Distributed Cache Manager
 * 
 * Two-tier caching:
 * - L1: Caffeine (local, in-memory)
 * - L2: Redis (distributed, shared)
 * 
 * Cache invalidation propagates across cluster via Redis pub/sub
 */
@ApplicationScoped
public class CacheManager {

    private static final Logger log = LoggerFactory.getLogger(CacheManager.class);
    private static final String INVALIDATION_CHANNEL = "cache:invalidation";

    @Inject
    @CacheName("workflow-run")
    Cache workflowRunCache;

    @Inject
    RedisDataSource redis;

    private ValueCommands<String, String> valueCommands;

    @jakarta.annotation.PostConstruct
    void init() {
        this.valueCommands = redis.value(String.class, String.class);

        // Subscribe to invalidation events
        subscribeToInvalidations();
    }

    /**
     * Invalidate cache entry and notify cluster
     */
    public void invalidate(String cacheName, String key) {
        log.debug("Invalidating cache: {}, key: {}", cacheName, key);

        // Invalidate local cache
        if ("workflow-run".equals(cacheName)) {
            workflowRunCache.invalidate(key).await().indefinitely();
        }

        // Notify cluster
        publishInvalidation(cacheName, key);
    }

    /**
     * Invalidate all entries for a cache
     */
    public void invalidateAll(String cacheName) {
        log.info("Invalidating all entries for cache: {}", cacheName);

        if ("workflow-run".equals(cacheName)) {
            workflowRunCache.invalidateAll().await().indefinitely();
        }

        publishInvalidation(cacheName, "*");
    }

    /**
     * Put value in distributed cache (Redis)
     */
    public Uni<Void> putDistributed(String key, String value, Duration ttl) {
        return Uni.createFrom().item(() -> {
            valueCommands.setex(key, ttl.getSeconds(), value);
            return null;
        });
    }

    /**
     * Get value from distributed cache (Redis)
     */
    public Uni<String> getDistributed(String key) {
        return Uni.createFrom().item(() -> valueCommands.get(key));
    }

    /**
     * Publish cache invalidation event
     */
    private void publishInvalidation(String cacheName, String key) {
        String message = cacheName + ":" + key;
        redis.pubsub(String.class).publish(INVALIDATION_CHANNEL, message);
    }

    /**
     * Subscribe to cache invalidation events from other nodes
     */
    private void subscribeToInvalidations() {
        redis.pubsub(String.class)
                .subscribe(INVALIDATION_CHANNEL, message -> {
                    log.debug("Received invalidation event: {}", message);

                    String[] parts = message.split(":", 2);
                    if (parts.length == 2) {
                        String cacheName = parts[0];
                        String key = parts[1];

                        if ("*".equals(key)) {
                            invalidateAll(cacheName);
                        } else {
                            // Only invalidate local cache (already invalidated on sender)
                            if ("workflow-run".equals(cacheName)) {
                                workflowRunCache.invalidate(key).subscribe()
                                        .with(v -> log.debug("Cache invalidated: {}", key));
                            }
                        }
                    }
                });
    }
}