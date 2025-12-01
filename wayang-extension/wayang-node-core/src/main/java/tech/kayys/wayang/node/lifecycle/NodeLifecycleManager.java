package tech.kayys.wayang.node.core.lifecycle;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.node.core.Node;
import tech.kayys.wayang.node.core.exception.NodeException;
import tech.kayys.wayang.node.core.model.NodeConfig;
import tech.kayys.wayang.node.core.model.NodeDescriptor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the lifecycle of nodes from initialization to cleanup.
 * 
 * Lifecycle stages:
 * 1. CREATED - Node instance created
 * 2. LOADING - onLoad() being called
 * 3. LOADED - Ready for execution
 * 4. EXECUTING - Currently executing
 * 5. IDLE - Loaded but not executing
 * 6. UNLOADING - onUnload() being called
 * 7. UNLOADED - Cleaned up
 */
@ApplicationScoped
public class NodeLifecycleManager {
    
    private static final Logger LOG = LoggerFactory.getLogger(NodeLifecycleManager.class);
    
    private final Map<String, NodeLifecycleState> lifecycleStates;
    private final NodeInitializer initializer;
    private final NodeDestructor destructor;
    private final MeterRegistry meterRegistry;
    
    @Inject
    public NodeLifecycleManager(
        NodeInitializer initializer,
        NodeDestructor destructor,
        MeterRegistry meterRegistry
    ) {
        this.lifecycleStates = new ConcurrentHashMap<>();
        this.initializer = initializer;
        this.destructor = destructor;
        this.meterRegistry = meterRegistry;
    }
    
    /**
     * Initialize a node
     */
    public void initialize(Node node, NodeDescriptor descriptor, NodeConfig config) 
            throws NodeException {
        
        String nodeKey = descriptor.getQualifiedId();
        
        NodeLifecycleState state = lifecycleStates.computeIfAbsent(
            nodeKey, 
            key -> new NodeLifecycleState(nodeKey)
        );
        
        synchronized (state) {
            if (state.stage != LifecycleStage.CREATED) {
                throw new NodeException(
                    "Node is not in CREATED stage: " + state.stage
                );
            }
            
            state.transitionTo(LifecycleStage.LOADING);
            
            try {
                LOG.info("Initializing node: {}", nodeKey);
                
                // Run initializer
                initializer.initialize(node, descriptor, config);
                
                state.transitionTo(LifecycleStage.LOADED);
                
                meterRegistry.counter("node.lifecycle.initialized").increment();
                
                LOG.info("Node initialized successfully: {}", nodeKey);
                
            } catch (Exception e) {
                state.transitionTo(LifecycleStage.FAILED);
                state.lastError = e.getMessage();
                
                meterRegistry.counter("node.lifecycle.init_failed").increment();
                
                throw new NodeException("Failed to initialize node: " + nodeKey, e);
            }
        }
    }
    
    /**
     * Mark node as executing
     */
    public void markExecuting(String nodeKey) {
        NodeLifecycleState state = lifecycleStates.get(nodeKey);
        if (state != null) {
            synchronized (state) {
                if (state.stage == LifecycleStage.LOADED || state.stage == LifecycleStage.IDLE) {
                    state.transitionTo(LifecycleStage.EXECUTING);
                    state.executionCount++;
                }
            }
        }
    }
    
    /**
     * Mark node as idle after execution
     */
    public void markIdle(String nodeKey) {
        NodeLifecycleState state = lifecycleStates.get(nodeKey);
        if (state != null) {
            synchronized (state) {
                if (state.stage == LifecycleStage.EXECUTING) {
                    state.transitionTo(LifecycleStage.IDLE);
                }
            }
        }
    }
    
    /**
     * Unload a node
     */
    public void unload(Node node, NodeDescriptor descriptor) {
        String nodeKey = descriptor.getQualifiedId();
        
        NodeLifecycleState state = lifecycleStates.get(nodeKey);
        if (state == null) {
            LOG.warn("No lifecycle state found for node: {}", nodeKey);
            return;
        }
        
        synchronized (state) {
            if (state.stage == LifecycleStage.UNLOADED) {
                LOG.debug("Node already unloaded: {}", nodeKey);
                return;
            }
            
            state.transitionTo(LifecycleStage.UNLOADING);
            
            try {
                LOG.info("Unloading node: {}", nodeKey);
                
                // Run destructor
                destructor.destroy(node, descriptor);
                
                state.transitionTo(LifecycleStage.UNLOADED);
                
                meterRegistry.counter("node.lifecycle.unloaded").increment();
                
                LOG.info("Node unloaded successfully: {}", nodeKey);
                
            } catch (Exception e) {
                LOG.error("Error unloading node: " + nodeKey, e);
                state.lastError = e.getMessage();
            }
        }
    }
    
    /**
     * Get lifecycle state for a node
     */
    public NodeLifecycleState getState(String nodeKey) {
        return lifecycleStates.get(nodeKey);
    }
    
    /**
     * Get all lifecycle states
     */
    public Map<String, NodeLifecycleState> getAllStates() {
        return Map.copyOf(lifecycleStates);
    }
    
    /**
     * Lifecycle stage enumeration
     */
    public enum LifecycleStage {
        CREATED,
        LOADING,
        LOADED,
        EXECUTING,
        IDLE,
        UNLOADING,
        UNLOADED,
        FAILED
    }
    
    /**
     * Node lifecycle state
     */
    public static class NodeLifecycleState {
        private final String nodeKey;
        private volatile LifecycleStage stage;
        private volatile Instant lastTransition;
        private volatile int executionCount;
        private volatile String lastError;
        
        public NodeLifecycleState(String nodeKey) {
            this.nodeKey = nodeKey;
            this.stage = LifecycleStage.CREATED;
            this.lastTransition = Instant.now();
            this.executionCount = 0;
        }
        
        void transitionTo(LifecycleStage newStage) {
            this.stage = newStage;
            this.lastTransition = Instant.now();
        }
        
        public String getNodeKey() { return nodeKey; }
        public LifecycleStage getStage() { return stage; }
        public Instant getLastTransition() { return lastTransition; }
        public int getExecutionCount() { return executionCount; }
        public String getLastError() { return lastError; }
    }
}