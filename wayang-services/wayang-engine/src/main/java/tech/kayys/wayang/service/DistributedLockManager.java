package tech.kayys.wayang.service;

import io.quarkus.redis.client.RedisClient;
import io.vertx.redis.client.RedisAPI;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.exception.WorkflowLockedException;
import tech.kayys.wayang.model.Lock;
import tech.kayys.wayang.model.RedisLock;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;

// Lock Manager for Concurrent Editing
@ApplicationScoped
public class DistributedLockManager {
    @Inject
    RedisAPI redisClient;

    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(5);

    public Lock acquireLock(UUID workflowId, String userId) {
        String lockKey = "workflow:lock:" + workflowId;
        String lockValue = userId + ":" + System.currentTimeMillis();

        // Use SET key value NX EX seconds
        io.vertx.redis.client.Response response = redisClient.set(Arrays.asList(
                lockKey,
                lockValue,
                "NX",
                "EX",
                String.valueOf(LOCK_TIMEOUT.toSeconds()))).toCompletionStage().toCompletableFuture().join();

        boolean acquired = response != null && "OK".equalsIgnoreCase(response.toString());

        if (!acquired) {
            throw new WorkflowLockedException(workflowId, "Lock is already held by another session");
        }

        return new RedisLock(lockKey, lockValue, (RedisClient) redisClient);
    }
}