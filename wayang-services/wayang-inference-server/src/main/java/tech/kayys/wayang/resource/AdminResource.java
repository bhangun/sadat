package tech.kayys.wayang.resource;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import tech.kayys.wayang.plugin.ModelManager;
import tech.kayys.wayang.service.RequestQueue;
import tech.kayys.wayang.service.ResponseCache;

public class AdminResource {
    
    @Inject
    ModelManager modelManager;
    
    @Inject
    ResponseCache cache;
    
    @Inject
    RequestQueue queue;
    
    @GET
    @Path("/stats")
    public Response getStats() {
        var model = modelManager.getActiveModel();
        var metrics = model.getMetrics();
        
        return Response.ok(Map.of(
            "model", model.getModelInfo(),
            "metrics", metrics.getAllMetrics(),
            "circuit_breaker", model.getCircuitBreaker().getCircuitState(),
            "cache", cache.getStats(),
            "queue", queue.getStats()
        )).build();
    }
    
    @POST
    @Path("/cache/clear")
    public Response clearCache() {
        cache.clear();
        return Response.ok(Map.of("status", "cleared")).build();
    }
    
    @POST
    @Path("/model/reload")
    public Response reloadModel() {
        try {
            // Implement model reload logic
            return Response.ok(Map.of("status", "reloaded")).build();
        } catch (Exception e) {
            return Response.status(500)
                .entity(Map.of("error", e.getMessage()))
                .build();
        }
    }
    
    @GET
    @Path("/threads")
    public Response getThreadInfo() {
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        ThreadGroup parentGroup;
        while ((parentGroup = rootGroup.getParent()) != null) {
            rootGroup = parentGroup;
        }
        
        int threadCount = rootGroup.activeCount();
        Thread[] threads = new Thread[threadCount];
        rootGroup.enumerate(threads);
        
        return Response.ok(Map.of(
            "thread_count", threadCount,
            "threads", java.util.Arrays.stream(threads)
                .filter(t -> t != null)
                .map(t -> Map.of(
                    "name", t.getName(),
                    "state", t.getState().toString(),
                    "daemon", t.isDaemon()
                ))
                .toList()
        )).build();
    }
    
    @GET
    @Path("/memory")
    public Response getMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        
        return Response.ok(Map.of(
            "max_memory_mb", runtime.maxMemory() / 1024 / 1024,
            "total_memory_mb", runtime.totalMemory() / 1024 / 1024,
            "free_memory_mb", runtime.freeMemory() / 1024 / 1024,
            "used_memory_mb", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        )).build();
    }
    
    @POST
    @Path("/gc")
    public Response triggerGC() {
        System.gc();
        return Response.ok(Map.of("status", "gc_triggered")).build();
    }
}
