package tech.kayys.wayang.plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Response;

public class MonitoringPlugin implements ServerPlugin {
    private PluginContext context;
    private final Map<String, EndpointMetrics> metrics = new ConcurrentHashMap<>();
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    
    @Override
    public String getId() {
        return "monitoring";
    }
    
    @Override
    public String getName() {
        return "Monitoring Plugin";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getDescription() {
        return "Real-time API monitoring and metrics";
    }
    
    @Override
    public void initialize(PluginContext context) {
        this.context = context;
    }
    
    @Override
    public void start() {}
    
    @Override
    public void stop() {
        metrics.clear();
    }
    
    @Override
    public void registerEndpoints(EndpointRegistry registry) {
        registry.registerGet("/plugins/monitoring/metrics", ctx -> {
            return Response.ok(Map.of(
                "total_requests", totalRequests.get(),
                "total_errors", totalErrors.get(),
                "endpoints", metrics
            )).build();
        });
        
        registry.registerGet("/plugins/monitoring/health", ctx -> {
            double errorRate = totalRequests.get() > 0 ? 
                (double) totalErrors.get() / totalRequests.get() : 0.0;
            
            return Response.ok(Map.of(
                "healthy", errorRate < 0.1,
                "error_rate", errorRate,
                "uptime_seconds", System.currentTimeMillis() / 1000
            )).build();
        });
    }
    
    @Override
    public void beforeRequest(ContainerRequestContext requestContext) {
        requestContext.setProperty("start_time", System.currentTimeMillis());
        totalRequests.incrementAndGet();
    }
    
    @Override
    public void afterResponse(ContainerRequestContext requestContext,
                             ContainerResponseContext responseContext) {
        Long startTime = (Long) requestContext.getProperty("start_time");
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            String path = requestContext.getUriInfo().getPath();
            
            EndpointMetrics endpointMetrics = metrics.computeIfAbsent(
                path, k -> new EndpointMetrics()
            );
            
            endpointMetrics.recordRequest(duration, responseContext.getStatus());
            
            if (responseContext.getStatus() >= 400) {
                totalErrors.incrementAndGet();
            }
        }
    }
    
    private static class EndpointMetrics {
        private final AtomicLong requestCount = new AtomicLong(0);
        private final AtomicLong totalDuration = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        
        void recordRequest(long duration, int status) {
            requestCount.incrementAndGet();
            totalDuration.addAndGet(duration);
            if (status >= 400) {
                errorCount.incrementAndGet();
            }
        }
        
        public Map<String, Object> toMap() {
            long requests = requestCount.get();
            return Map.of(
                "request_count", requests,
                "average_duration_ms", requests > 0 ? totalDuration.get() / requests : 0,
                "error_count", errorCount.get()
            );
        }
    }
}
