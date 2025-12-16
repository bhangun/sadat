package tech.kayys.wayang.workflow.service;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.workflow.model.ExecutionContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context manager for managing execution contexts
 */

@ApplicationScoped
public class ExecutionContextManager {

    private final Map<String, ExecutionContext> contexts = new ConcurrentHashMap<>();

    /**
     * Create new execution context
     */
    public ExecutionContext createContext() {
        ExecutionContext context = new ExecutionContext();
        contexts.put(context.getExecutionId(), context);
        return context;
    }

    /**
     * Get execution context by ID
     */
    public ExecutionContext getContext(String executionId) {
        return contexts.get(executionId);
    }

    /**
     * Remove execution context
     */
    public void removeContext(String executionId) {
        contexts.remove(executionId);
    }

    /**
     * Clean up old contexts (can be called periodically)
     */
    public void cleanupOldContexts(long maxAgeMillis) {
        long now = System.currentTimeMillis();
        contexts.entrySet().removeIf(entry -> now - entry.getValue().getExecutionDuration() > maxAgeMillis);
    }
}