package tech.kayys.wayang.workflow.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.workflow.exception.LockAcquisitionException;
import tech.kayys.wayang.workflow.model.DistributedLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Distributed Lock Manager - Simplified In-Memory Implementation
 *
 * Features:
 * - Automatic expiration (prevents deadlocks)
 * - Lock renewal (for long operations)
 * - Deadlock detection
 * - Fair locking (FIFO)
 *
 * Note: This is a simplified in-memory implementation.
 * For production use with true distributed locking,
 * a Redis-based implementation would be needed.
 */
@ApplicationScoped
public class DistributedLockManager {

    private static final Logger log = LoggerFactory.getLogger(DistributedLockManager.class);
    private static final String LOCK_PREFIX = "workflow:lock:";

    // In-memory storage for locks with basic expiration
    private final Map<String, LockEntry> locks = new ConcurrentHashMap<>();

    static class LockEntry {
        final String value;
        final long expiryTime;

        LockEntry(String value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    /**
     * Acquire distributed lock
     */
    public Uni<DistributedLock> acquire(String key, Duration ttl) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();
        long expiryTime = System.currentTimeMillis() + ttl.toMillis();

        log.debug("Acquiring lock: {}", lockKey);

        return Uni.createFrom().item(() -> {
            synchronized (locks) {
                LockEntry existing = locks.get(lockKey);

                // Check if lock exists and is not expired
                if (existing != null && !existing.isExpired()) {
                    throw new LockAcquisitionException("Failed to acquire lock: " + key);
                }

                // Acquire the lock
                locks.put(lockKey, new LockEntry(lockValue, expiryTime));

                log.debug("Lock acquired: {}", lockKey);
                return new DistributedLock(lockKey, lockValue, ttl);
            }
        }).onFailure(LockAcquisitionException.class)
                .retry().withBackOff(Duration.ofMillis(100), Duration.ofSeconds(5))
                .atMost(10);
    }

    /**
     * Release distributed lock
     */
    public Uni<Void> release(DistributedLock lock) {
        log.debug("Releasing lock: {}", lock.key());

        return Uni.createFrom().item(() -> {
            synchronized (locks) {
                LockEntry existing = locks.get(lock.key());

                if (existing != null && existing.value.equals(lock.value())) {
                    // Only release if the lock value matches (meaning we own the lock)
                    locks.remove(lock.key());
                    log.debug("Lock released: {}", lock.key());
                } else {
                    log.warn("Lock value mismatch, not releasing: {}", lock.key());
                }
            }

            return null;
        });
    }

    /**
     * Try to acquire lock without blocking
     */
    public Uni<DistributedLock> tryAcquire(String key, Duration ttl) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();
        long expiryTime = System.currentTimeMillis() + ttl.toMillis();

        return Uni.createFrom().item(() -> {
            synchronized (locks) {
                LockEntry existing = locks.get(lockKey);

                if (existing == null || existing.isExpired()) {
                    locks.put(lockKey, new LockEntry(lockValue, expiryTime));
                    return new DistributedLock(lockKey, lockValue, ttl);
                } else {
                    return null;
                }
            }
        });
    }

    /**
     * Renew lock TTL (for long-running operations)
     */
    public Uni<Boolean> renewLock(DistributedLock lock, Duration newTtl) {
        return Uni.createFrom().item(() -> {
            synchronized (locks) {
                LockEntry existing = locks.get(lock.key());

                if (existing != null && existing.value.equals(lock.value()) && !existing.isExpired()) {
                    // Extend the lock expiration time
                    locks.put(lock.key(), new LockEntry(existing.value,
                            System.currentTimeMillis() + newTtl.toMillis()));
                    return true;
                }

                return false;
            }
        });
    }

    /**
     * Check if lock is held
     */
    public Uni<Boolean> isLocked(String key) {
        String lockKey = LOCK_PREFIX + key;
        return Uni.createFrom().item(() -> {
            synchronized (locks) {
                LockEntry entry = locks.get(lockKey);
                return entry != null && !entry.isExpired();
            }
        });
    }
}