
// Lock Manager for Concurrent Editing
@ApplicationScoped
public class DistributedLockManager {
    @Inject RedisClient redisClient;
    
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(5);
    
    public Lock acquireLock(UUID workflowId, String userId) {
        String lockKey = "workflow:lock:" + workflowId;
        String lockValue = userId + ":" + System.currentTimeMillis();
        
        boolean acquired = redisClient.setnx(
            lockKey,
            lockValue,
            LOCK_TIMEOUT.toSeconds()
        );
        
        if (!acquired) {
            // Check if lock is stale
            String existingLock = redisClient.get(lockKey);
            if (existingLock != null && isLockStale(existingLock)) {
                // Force acquire
                redisClient.set(lockKey, lockValue, LOCK_TIMEOUT.toSeconds());
                acquired = true;
            }
        }
        
        if (!acquired) {
            throw new WorkflowLockedException(workflowId);
        }
        
        return new RedisLock(lockKey, lockValue, redisClient);
    }
}