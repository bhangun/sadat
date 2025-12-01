package tech.kayys.wayang.node.core.factory;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.quarkus.arc.All;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.node.core.Node;
import tech.kayys.wayang.node.core.exception.NodeFactoryException;
import tech.kayys.wayang.node.core.model.ImplementationType;
import tech.kayys.wayang.node.core.model.NodeDescriptor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for node factories.
 * 
 * Routes node creation requests to appropriate factory based on implementation type.
 * Provides caching, metrics, and validation.
 */
@ApplicationScoped
public class NodeFactoryRegistry {
    
    private static final Logger LOG = LoggerFactory.getLogger(NodeFactoryRegistry.class);
    
    private final Map<ImplementationType, NodeFactory> factories;
    private final MeterRegistry meterRegistry;
    private final Map<String, Node> nodeCache;
    
    @Inject
    public NodeFactoryRegistry(
        @All List<NodeFactory> factoryList,
        MeterRegistry meterRegistry
    ) {
        this.meterRegistry = meterRegistry;
        this.factories = new ConcurrentHashMap<>();
        this.nodeCache = new ConcurrentHashMap<>();
        
        // Register all discovered factories
        for (NodeFactory factory : factoryList) {
            ImplementationType type = factory.getImplementationType();
            factories.put(type, factory);
            LOG.info("Registered node factory for type: {}", type);
        }
    }
    
    /**
     * Create a node from a descriptor.
     * Uses caching to avoid recreating identical nodes.
     * 
     * @param descriptor The node descriptor
     * @param useCache Whether to use cached instances
     * @return The created node
     * @throws NodeFactoryException if creation fails
     */
    public Node createNode(NodeDescriptor descriptor, boolean useCache) 
            throws NodeFactoryException {
        
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            // Check cache first if enabled
            if (useCache) {
                String cacheKey = descriptor.getQualifiedId();
                Node cached = nodeCache.get(cacheKey);
                if (cached != null) {
                    LOG.debug("Using cached node: {}", cacheKey);
                    meterRegistry.counter("node.factory.cache.hit").increment();
                    return cached;
                }
                meterRegistry.counter("node.factory.cache.miss").increment();
            }
            
            // Get appropriate factory
            NodeFactory factory = getFactory(descriptor);
            
            // Validate descriptor
            factory.validate(descriptor);
            
            // Create node
            Node node = factory.create(descriptor);
            
            // Cache if enabled
            if (useCache) {
                nodeCache.put(descriptor.getQualifiedId(), node);
            }
            
            LOG.info("Created node: {} (type: {})", 
                descriptor.getQualifiedId(), 
                descriptor.implementation().type());
            
            return node;
            
        } finally {
            sample.stop(Timer.builder("node.factory.create")
                .tag("type", descriptor.implementation().type().name())
                .register(meterRegistry));
        }
    }
    
    /**
     * Create a node without caching
     */
    public Node createNode(NodeDescriptor descriptor) throws NodeFactoryException {
        return createNode(descriptor, false);
    }
    
    /**
     * Get the appropriate factory for a descriptor
     */
    private NodeFactory getFactory(NodeDescriptor descriptor) throws NodeFactoryException {
        ImplementationType type = descriptor.implementation().type();
        NodeFactory factory = factories.get(type);
        
        if (factory == null) {
            throw new NodeFactoryException(
                "No factory registered for implementation type: " + type
            );
        }
        
        if (!factory.supports(descriptor)) {
            throw new NodeFactoryException(
                "Factory does not support descriptor: " + descriptor.getQualifiedId()
            );
        }
        
        return factory;
    }
    
    /**
     * Clear the node cache
     */
    public void clearCache() {
        LOG.info("Clearing node cache ({} entries)", nodeCache.size());
        nodeCache.clear();
    }
    
    /**
     * Remove a specific node from cache
     */
    public void evictFromCache(String qualifiedId) {
        nodeCache.remove(qualifiedId);
        LOG.debug("Evicted node from cache: {}", qualifiedId);
    }
    
    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
            "size", nodeCache.size(),
            "keys", List.copyOf(nodeCache.keySet())
        );
    }
}