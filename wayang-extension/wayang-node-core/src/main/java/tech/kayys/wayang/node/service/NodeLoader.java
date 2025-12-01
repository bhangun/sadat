package tech.kayys.wayang.node.core.loader;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.arc.All;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.node.core.Node;
import tech.kayys.wayang.node.core.exception.NodeLoadException;
import tech.kayys.wayang.node.core.model.NodeDescriptor;
import tech.kayys.wayang.node.core.model.SandboxLevel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages node loading with appropriate isolation strategies.
 * 
 * Routes to different loaders based on sandbox level:
 * - TRUSTED: ClassLoader isolation (fast)
 * - SEMI_TRUSTED: Enhanced ClassLoader with security manager
 * - UNTRUSTED: WASM or Container isolation
 */
@ApplicationScoped
public class NodeLoader {
    
    private static final Logger LOG = LoggerFactory.getLogger(NodeLoader.class);
    
    private final Map<SandboxLevel, NodeLoaderStrategy> strategies;
    private final MeterRegistry meterRegistry;
    
    @Inject
    public NodeLoader(
        @All List<NodeLoaderStrategy> strategyList,
        MeterRegistry meterRegistry
    ) {
        this.meterRegistry = meterRegistry;
        this.strategies = new ConcurrentHashMap<>();
        
        // Register all strategies
        for (NodeLoaderStrategy strategy : strategyList) {
            SandboxLevel level = strategy.getSandboxLevel();
            strategies.put(level, strategy);
            LOG.info("Registered loader strategy for sandbox level: {}", level);
        }
    }
    
    /**
     * Load a node with appropriate isolation
     */
    public Node load(NodeDescriptor descriptor, Node nodeInstance) 
            throws NodeLoadException {
        
        SandboxLevel sandboxLevel = descriptor.sandboxLevel();
        
        NodeLoaderStrategy strategy = strategies.get(sandboxLevel);
        if (strategy == null) {
            throw new NodeLoadException(
                "No loader strategy found for sandbox level: " + sandboxLevel
            );
        }
        
        try {
            LOG.info("Loading node {} with sandbox level: {}", 
                descriptor.getQualifiedId(), sandboxLevel);
            
            Node loadedNode = strategy.load(descriptor, nodeInstance);
            
            meterRegistry.counter("node.loader.load.success",
                "sandbox_level", sandboxLevel.name()).increment();
            
            return loadedNode;
            
        } catch (Exception e) {
            meterRegistry.counter("node.loader.load.failure",
                "sandbox_level", sandboxLevel.name()).increment();
            throw new NodeLoadException(
                "Failed to load node: " + descriptor.getQualifiedId(), 
                e
            );
        }
    }
    
    /**
     * Unload a node
     */
    public void unload(NodeDescriptor descriptor, Node node) {
        SandboxLevel sandboxLevel = descriptor.sandboxLevel();
        
        NodeLoaderStrategy strategy = strategies.get(sandboxLevel);
        if (strategy != null) {
            try {
                strategy.unload(descriptor, node);
                LOG.info("Unloaded node: {}", descriptor.getQualifiedId());
            } catch (Exception e) {
                LOG.error("Error unloading node: " + descriptor.getQualifiedId(), e);
            }
        }
    }
}