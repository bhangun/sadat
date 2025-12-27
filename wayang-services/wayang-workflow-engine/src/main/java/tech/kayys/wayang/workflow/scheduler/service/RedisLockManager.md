package tech.kayys.wayang.workflow.scheduler;

import io.quarkus.redis.datasource.RedisDataSource;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RedisLockManager {

    @Inject
    RedisDataSource redis;

    public Uni<Boolean> acquireLock(String key, long ttlSeconds) {
        return redis.value(String.class)
                .setnx(key, "locked")
                .onItem().transformToUni(acquired -> {
                    if (acquired) {
                        return redis.key().expire(key, ttlSeconds)
                                .map(v -> true);
                    }
                    return Uni.createFrom().item(false);
                });
    }

    public Uni<Void> releaseLock(String key) {
        return redis.key().del(key).replaceWithVoid();
    }
}
