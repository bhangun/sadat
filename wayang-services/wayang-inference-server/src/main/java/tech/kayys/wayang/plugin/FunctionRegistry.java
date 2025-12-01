package tech.kayys.wayang.plugin;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import tech.kayys.wayang.mcp.Metrics;
import tech.kayys.wayang.model.Tool;
import tech.kayys.wayang.service.EngineService;

public class FunctionRegistry {

      private static final Logger log = Logger.getLogger(EngineService.class);
    
    private final Map<String, RegisteredFunction> functions = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicLong totalExecutions = new AtomicLong(0);
    private final AtomicLong failedExecutions = new AtomicLong(0);
    
    @Inject
    Metrics metrics;
    
    public void register(Tool tool, Function<JsonNode, String> handler) {
        register(tool, handler, true);
    }
    
    public void register(Tool tool, Function<JsonNode, String> handler, boolean overwrite) {
        if (tool == null || handler == null) {
            throw new IllegalArgumentException("Tool and handler cannot be null");
        }
        
        String functionName = tool.function().name();
        lock.writeLock().lock();
        try {
            if (!overwrite && functions.containsKey(functionName)) {
                throw new IllegalArgumentException("Function already registered: " + functionName);
            }
            
            functions.put(functionName, new RegisteredFunction(tool, handler));
            metrics.incrementCounter("functions.registered");
            metrics.setGauge("functions.total", functions.size());
            
            log.infof("Function registered: {}", functionName);
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public String execute(String functionName, JsonNode arguments) {
        return execute(functionName, arguments, Duration.ofSeconds(30)); // Default timeout
    }
    
    public String execute(String functionName, JsonNode arguments, Duration timeout) {
        long startTime = System.currentTimeMillis();
        totalExecutions.incrementAndGet();
        metrics.incrementCounter("functions.executed");
        
        RegisteredFunction func = functions.get(functionName);
        if (func == null) {
            failedExecutions.incrementAndGet();
            metrics.incrementCounter("functions.execution_failures");
            throw new FunctionNotFoundException("Unknown function: " + functionName);
        }
        
        try {
            // Validate arguments against function schema
            validateArguments(func.tool().function().parameters(), arguments);
            
            // Execute with timeout
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> 
                func.handler().apply(arguments));
            
            String result = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            
            long duration = System.currentTimeMillis() - startTime;
            metrics.recordHistogram("functions.execution_time_ms", duration);
            
            return result;
            
        } catch (TimeoutException e) {
            failedExecutions.incrementAndGet();
            metrics.incrementCounter("functions.timeouts");
            throw new FunctionTimeoutException("Function execution timed out: " + functionName, e);
        } catch (ExecutionException e) {
            failedExecutions.incrementAndGet();
            metrics.incrementCounter("functions.execution_errors");
            throw new FunctionExecutionException("Function execution failed: " + functionName, e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            failedExecutions.incrementAndGet();
            metrics.incrementCounter("functions.interrupted");
            throw new FunctionExecutionException("Function execution interrupted: " + functionName, e);
        } catch (Exception e) {
            failedExecutions.incrementAndGet();
            metrics.incrementCounter("functions.execution_failures");
            throw new FunctionExecutionException("Function execution failed: " + functionName, e);
        }
    }
    
    private void validateArguments(JsonNode parametersSchema, JsonNode arguments) {
        // Basic validation - you could integrate with a JSON schema validator
        if (parametersSchema == null || arguments == null) {
            return;
        }
        
        // Check if arguments is an object
        if (!arguments.isObject()) {
            throw new IllegalArgumentException("Arguments must be a JSON object");
        }
        
        // TODO: Add more comprehensive schema validation
        // This could use a library like everit-org/json-schema or networknt/json-schema-validator
    }
    
    public List<Tool> getTools() {
        lock.readLock().lock();
        try {
            return functions.values().stream()
                .map(RegisteredFunction::tool)
                .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public boolean hasFunction(String name) {
        lock.readLock().lock();
        try {
            return functions.containsKey(name);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void unregister(String functionName) {
        lock.writeLock().lock();
        try {
            if (functions.remove(functionName) != null) {
                metrics.incrementCounter("functions.unregistered");
                metrics.setGauge("functions.total", functions.size());
                log.infof("Function unregistered: {}", functionName);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void clear() {
        lock.writeLock().lock();
        try {
            int count = functions.size();
            functions.clear();
            metrics.setGauge("functions.total", 0);
            log.infof("Cleared all {} functions", count);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public FunctionRegistryStats getStats() {
        return new FunctionRegistryStats(
            functions.size(),
            totalExecutions.get(),
            failedExecutions.get()
        );
    }
    
    private record RegisteredFunction(Tool tool, Function<JsonNode, String> handler) {}
    
    public record FunctionRegistryStats(
        int totalFunctions,
        long totalExecutions,
        long failedExecutions
    ) {}
    
    // Custom exceptions
    public static class FunctionNotFoundException extends RuntimeException {
        public FunctionNotFoundException(String message) { super(message); }
    }
    
    public static class FunctionTimeoutException extends RuntimeException {
        public FunctionTimeoutException(String message, Throwable cause) { super(message, cause); }
    }
    
    public static class FunctionExecutionException extends RuntimeException {
        public FunctionExecutionException(String message, Throwable cause) { super(message, cause); }
    }
}