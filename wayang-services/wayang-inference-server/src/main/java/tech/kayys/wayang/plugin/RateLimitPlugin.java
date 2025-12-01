package tech.kayys.wayang.plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ws.rs.container.ContainerRequestContext;

public class RateLimitPlugin implements ServerPlugin {
    private PluginContext context;
    private final Map<String, RateLimiter> limiters = new ConcurrentHashMap<>();
    private int requestsPerMinute = 60;
    
    @Override
    public String getId() {
        return "rate-limit";
    }
    
    @Override
    public String getName() {
        return "Rate Limit Plugin";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "Rate limiting for API requests";
    }
    
    @Override
    public void initialize(PluginContext context) throws PluginException {
        this.context = context;
        
        Integer configLimit = context.getConfigValue("rate_limit.requests_per_minute", Integer.class);
        if (configLimit != null) {
            requestsPerMinute = configLimit;
        }
    }
    
    @Override
    public void start() {
        // Start cleanup thread
        startCleanupThread();
    }
    
    @Override
    public void stop() {}
    
    @Override
    public void beforeRequest(ContainerRequestContext requestContext) {
        String clientIp = getClientIp(requestContext);
        RateLimiter limiter = limiters.computeIfAbsent(clientIp, k -> new RateLimiter(requestsPerMinute));
        
        if (!limiter.allowRequest()) {
            requestContext.abortWith(
                jakarta.ws.rs.core.Response.status(429)
                    .entity("Rate limit exceeded")
                    .header("X-RateLimit-Limit", requestsPerMinute)
                    .header("X-RateLimit-Remaining", 0)
                    .header("Retry-After", 60)
                    .build()
            );
        }
    }
    
    private String getClientIp(ContainerRequestContext requestContext) {
        String forwarded = requestContext.getHeaderString("X-Forwarded-For");
        if (forwarded != null) {
            return forwarded.split(",")[0].trim();
        }
        return requestContext.getUriInfo().getRequestUri().getHost();
    }
    
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(60000); // 1 minute
                    limiters.values().forEach(RateLimiter::reset);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
    
    private static class RateLimiter {
        private final int maxRequests;
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private long windowStart = System.currentTimeMillis();
        
        RateLimiter(int maxRequests) {
            this.maxRequests = maxRequests;
        }
        
        synchronized boolean allowRequest() {
            long now = System.currentTimeMillis();
            if (now - windowStart > 60000) {
                reset();
            }
            
            return requestCount.incrementAndGet() <= maxRequests;
        }
        
        void reset() {
            requestCount.set(0);
            windowStart = System.currentTimeMillis();
        }
    }
}
