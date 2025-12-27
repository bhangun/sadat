package tech.kayys.wayang.workflow.scheduler.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * ScheduleLockManager: Distributed locking for schedule execution.
 * 
 * Implementation Notes:
 * - In-memory locks for demonstration
 * - Production: Use Redis or Hazelcast for distributed locks
 * - Prevents duplicate execution in clustered environments
 */
@ApplicationScoped
public class ScheduleLockManager {

    private static final Logger LOG = Logger.getLogger(ScheduleLockManager.class);

    private final Map<String, LockEntry> locks = new ConcurrentHashMap<>();

    /**
     * Acquire a lock for the given key.
     * 
     * @param key        Lock identifier
     * @param ttlSeconds Time-to-live in seconds
     * @return true if lock acquired, false otherwise
     */
    public Uni<Boolean> acquireLock(String key, long ttlSeconds) {
        return Uni.createFrom().item(() -> {
            long now = System.currentTimeMillis();
            long expiry = now + TimeUnit.SECONDS.toMillis(ttlSeconds);

            LockEntry entry = locks.computeIfAbsent(key, k -> new LockEntry(expiry));

            // Check if existing lock is expired
            if (entry.expiry < now) {
                // Lock expired, acquire new one
                locks.put(key, new LockEntry(expiry));
                LOG.debugf("Acquired lock: %s", key);
                return true;
            }

            // Lock still valid
            LOG.debugf("Lock already held: %s", key);
            return false;
        });
    }

    /**
     * Release a lock.
     */
    public Uni<Void> releaseLock(String key) {
        return Uni.createFrom().item(() -> {
            locks.remove(key);
            LOG.debugf("Released lock: %s", key);
            return null;
        });
    }

    private record LockEntry(long expiry) {
    }
}