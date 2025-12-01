package tech.kayys.wayang.service;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;

import tech.kayys.wayang.model.GenerationResult;

public class ResponseCache {
    private static final Logger log = Logger.getLogger(ResponseCache.class);
    
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newScheduledThreadPool(1);
    private final int maxSize = 1000;
    private final long ttlMs = 3600000; // 1 hour
    
    public ResponseCache() {
        startCleanup();
    }
    
    private void startCleanup() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanup();
            } catch (Exception e) {
                log.error("Cache cleanup failed", e);
            }
        }, 5, 5, TimeUnit.MINUTES);
    }
    
    public GenerationResult get(String prompt, Map<String, Object> params) {
        String key = generateKey(prompt, params);
        CacheEntry entry = cache.get(key);
        
        if (entry != null && !entry.isExpired()) {
            log.debugf("Cache hit: %s", key.substring(0, 8));
            return entry.result;
        }
        
        return null;
    }
    
    public void put(String prompt, Map<String, Object> params, GenerationResult result) {
        if (cache.size() >= maxSize) {
            evictOldest();
        }
        
        String key = generateKey(prompt, params);
        cache.put(key, new CacheEntry(result, System.currentTimeMillis(), ttlMs));
        
        log.debugf("Cached response: %s", key.substring(0, 8));
    }
    
    private String generateKey(String prompt, Map<String, Object> params) {
        try {
            String combined = prompt + params.toString();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(combined.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return String.valueOf(Objects.hash(prompt, params));
        }
    }
    
    private void cleanup() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired());
        log.debugf("Cache cleanup: %d entries remaining", cache.size());
    }
    
    private void evictOldest() {
        cache.entrySet().stream()
            .min(Comparator.comparingLong(e -> e.getValue().timestamp))
            .ifPresent(entry -> cache.remove(entry.getKey()));
    }
    
    public CacheStats getStats() {
        int expired = (int) cache.values().stream()
            .filter(CacheEntry::isExpired)
            .count();
        
        return new CacheStats(
            cache.size(),
            expired,
            maxSize
        );
    }
    
    public void clear() {
        cache.clear();
        log.info("Cache cleared");
    }
    
    private record CacheEntry(
        GenerationResult result,
        long timestamp,
        long ttl
    ) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > ttl;
        }
    }
    
    public record CacheStats(
        int size,
        int expired,
        int maxSize
    ) {}
}
