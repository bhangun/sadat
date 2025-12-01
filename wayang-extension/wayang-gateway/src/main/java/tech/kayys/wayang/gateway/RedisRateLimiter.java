package tech.kayys.wayang.gateway.filter;

import io.github.bucket4j.*;
import io.quarkus.cache.CacheResult;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Provider
@PreMatching
@Priority(2000)
public class RateLimitFilter implements ContainerRequestFilter {
    
    @Inject
    BucketRegistry bucketRegistry;
    
    @Override
    public void filter(ContainerRequestContext requestContext) {
        String tenantId = requestContext.getHeaderString("X-Tenant-ID");
        
        if (tenantId == null) {
            return; // Will be caught by auth filter
        }
        
        Bucket bucket = bucketRegistry.getBucket(tenantId);
        
        if (!bucket.tryConsume(1)) {
            requestContext.abortWith(
                Response.status(429) // Too Many Requests
                    .entity("Rate limit exceeded")
                    .header("X-Rate-Limit-Retry-After", "60")
                    .build()
            );
        }
    }
}


// Rate Limiter
@ApplicationScoped
public class RedisRateLimiter {
    @Inject RedisClient redisClient;
    
    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    
    public boolean allowRequest(UUID tenantId) {
        String key = "rate_limit:" + tenantId + ":" + getCurrentMinute();
        
        Long current = redisClient.incr(key);
        
        if (current == 1) {
            // First request in this minute
            redisClient.expire(key, 60);
        }
        
        return current <= MAX_REQUESTS_PER_MINUTE;
    }
    
    private String getCurrentMinute() {
        return Instant.now().truncatedTo(ChronoUnit.MINUTES).toString();
    }
}