package tech.kayys.wayang.node.core.model;

import io.opentelemetry.api.trace.Span;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Execution context for a node, containing runtime state, metadata,
 * tracing information, and access to platform services.
 * 
 * Thread-safe and designed for concurrent access during execution.
 */
public class NodeContext {
    
    private final String contextId;
    private final String runId;
    private final String nodeId;
    private final String tenantId;
    private final String traceId;
    private final Instant createdAt;

    
    // Mutable state - thread-safe
    private final Map<String, Object> variables;
  
    
    // Execution metadata
    private final ExecutionMetadata executionMetadata;
    
    // OpenTelemetry span
    private final Span span;
    
    // Service adapters (injected)
    private final Map<Class<?>, Object> services;


    private final Map<String, Object> inputs;
    private final Map<String, Object> variables;
    private final ExecutionMetadata metadata;
    private final ResourceBindings bindings;
    
    
    private NodeContext(Builder builder) {
        this.contextId = builder.contextId;
        this.runId = builder.runId;
        this.nodeId = builder.nodeId;
        this.tenantId = builder.tenantId;
        this.traceId = builder.traceId;
        this.createdAt = builder.createdAt;
        this.variables = new ConcurrentHashMap<>(builder.variables);
        this.metadata = new ConcurrentHashMap<>(builder.metadata);
        this.executionMetadata = builder.executionMetadata;
        this.span = builder.span;
        this.services = new ConcurrentHashMap<>(builder.services);
    }
    
    // Getters
    public String getContextId() { return contextId; }
    public String getRunId() { return runId; }
    public String getNodeId() { return nodeId; }
    public String getTenantId() { return tenantId; }
    public String getTraceId() { return traceId; }
    public Instant getCreatedAt() { return createdAt; }
    public ExecutionMetadata getExecutionMetadata() { return executionMetadata; }
    public Span getSpan() { return span; }
    
    /**
     * Get a variable from the context
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getVariable(String name, Class<T> type) {
        Object value = variables.get(name);
        if (value == null) {
            return Optional.empty();
        }
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }
        throw new IllegalArgumentException(
            "Variable " + name + " is not of type " + type.getName()
        );
    }
    
    /**
     * Set a variable in the context
     */
    public void setVariable(String name, Object value) {
        if (value == null) {
            variables.remove(name);
        } else {
            variables.put(name, value);
        }
    }
    
    /**
     * Get all variables (immutable view)
     */
    public Map<String, Object> getVariables() {
        return Map.copyOf(variables);
    }
    
    /**
     * Get metadata value
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }
    
    /**
     * Set metadata
     */
    public void setMetadata(String key, Object value) {
        if (value == null) {
            metadata.remove(key);
        } else {
            metadata.put(key, value);
        }
    }
    
    /**
     * Get a service adapter
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getService(Class<T> serviceClass) {
        Object service = services.get(serviceClass);
        if (service == null) {
            return Optional.empty();
        }
        return Optional.of((T) service);
    }
    
    /**
     * Register a service adapter
     */
    public <T> void registerService(Class<T> serviceClass, T service) {
        services.put(serviceClass, service);
    }
    
    /**
     * Create a child context for nested operations
     */
    public NodeContext createChildContext(String childNodeId) {
        return new Builder()
            .contextId(UUID.randomUUID().toString())
            .runId(this.runId)
            .nodeId(childNodeId)
            .tenantId(this.tenantId)
            .traceId(this.traceId)
            .createdAt(Instant.now())
            .variables(new ConcurrentHashMap<>(this.variables))
            .metadata(new ConcurrentHashMap<>(this.metadata))
            .executionMetadata(this.executionMetadata)
            .span(this.span)
            .services(new ConcurrentHashMap<>(this.services))
            .build();
    }
    
    /**
     * Builder for NodeContext
     */
    public static class Builder {
        private String contextId = UUID.randomUUID().toString();
        private String runId;
        private String nodeId;
        private String tenantId;
        private String traceId;
        private Instant createdAt = Instant.now();
        private Map<String, Object> variables = new ConcurrentHashMap<>();
        private Map<String, Object> metadata = new ConcurrentHashMap<>();
        private ExecutionMetadata executionMetadata;
        private Span span;
        private Map<Class<?>, Object> services = new ConcurrentHashMap<>();
        
        public Builder contextId(String contextId) {
            this.contextId = contextId;
            return this;
        }
        
        public Builder runId(String runId) {
            this.runId = runId;
            return this;
        }
        
        public Builder nodeId(String nodeId) {
            this.nodeId = nodeId;
            return this;
        }
        
        public Builder tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }
        
        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }
        
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Builder variables(Map<String, Object> variables) {
            this.variables = new ConcurrentHashMap<>(variables);
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = new ConcurrentHashMap<>(metadata);
            return this;
        }
        
        public Builder executionMetadata(ExecutionMetadata executionMetadata) {
            this.executionMetadata = executionMetadata;
            return this;
        }
        
        public Builder span(Span span) {
            this.span = span;
            return this;
        }
        
        public Builder services(Map<Class<?>, Object> services) {
            this.services = new ConcurrentHashMap<>(services);
            return this;
        }
        
        public NodeContext build() {
            return new NodeContext(this);
        }
    }
}


/**
 * Node context - execution context for a single node
 */
public final class NodeContext {
 
    // Implementation...
    
    public Object getInput(String name) {
        return inputs.get(name);
    }
    
    public <T> T getInput(String name, Class<T> type) {
        return type.cast(inputs.get(name));
    }
    
    public boolean hasInput(String name) {
        return inputs.containsKey(name);
    }
    
    public ResourceBinding getBinding(String name) {
        return bindings.get(name);
    }
}