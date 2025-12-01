package tech.kayys.wayang.service;

import java.util.Timer;
import java.util.concurrent.atomic.AtomicInteger;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Inject;

public class PlatformMetrics {
    
    @Inject
    MeterRegistry registry;
    
    private final AtomicInteger activeRequests = new AtomicInteger(0);
    
    public void recordRequest(String endpoint, String method) {
        Counter.builder("llama.requests.total")
            .tag("endpoint", endpoint)
            .tag("method", method)
            .register(registry)
            .increment();
    }
    
    public void recordRequestDuration(String endpoint, long durationMs) {
        Timer.builder("llama.request.duration")
            .tag("endpoint", endpoint)
            .register(registry)
            .record(java.time.Duration.ofMillis(durationMs));
    }
    
    public void recordTokensGenerated(int tokens) {
        Counter.builder("llama.tokens.generated.total")
            .register(registry)
            .increment(tokens);
    }
    
    public void recordInferenceTime(long timeMs) {
        Timer.builder("llama.inference.duration")
            .register(registry)
            .record(java.time.Duration.ofMillis(timeMs));
    }
    
    public void recordCacheHit(boolean hit) {
        Counter.builder("llama.cache." + (hit ? "hits" : "misses"))
            .register(registry)
            .increment();
    }
    
    public void setActiveRequests(int count) {
        activeRequests.set(count);
        Gauge.builder("llama.requests.active", activeRequests, AtomicInteger::get)
            .register(registry);
    }
    
    public void recordQueueSize(int size) {
        Gauge.builder("llama.queue.size", () -> size)
            .register(registry);
    }
    
    public void recordCircuitBreakerState(String state) {
        Gauge.builder("llama.circuit.breaker.state", () -> {
            return switch(state) {
                case "CLOSED" -> 0;
                case "HALF_OPEN" -> 1;
                case "OPEN" -> 2;
                default -> -1;
            };
        }).register(registry);
    }
}
