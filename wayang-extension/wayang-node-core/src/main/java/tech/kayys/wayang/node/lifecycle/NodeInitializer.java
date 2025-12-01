package tech.kayys.wayang.node.core.lifecycle;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.node.core.Node;
import tech.kayys.wayang.node.core.exception.NodeException;
import tech.kayys.wayang.node.core.model.NodeConfig;
import tech.kayys.wayang.node.core.model.NodeDescriptor;
import tech.kayys.wayang.node.core.validation.NodeValidator;

import java.util.List;

/**
 * Handles node initialization process.
 * 
 * Responsibilities:
 * - Pre-initialization validation
 * - Configuration preparation
 * - Resource allocation
 * - Post-initialization verification
 */
@ApplicationScoped
public class NodeInitializer {
    
    private static final Logger LOG = LoggerFactory.getLogger(NodeInitializer.class);
    
    private final List<NodeValidator> validators;
    
    public NodeInitializer(List<NodeValidator> validators) {
        this.validators = validators;
    }
    
    /**
     * Initialize a node with validation and setup
     */
    public void initialize(Node node, NodeDescriptor descriptor, NodeConfig config) 
            throws NodeException {
        
        LOG.debug("Starting initialization for node: {}", descriptor.getQualifiedId());
        
        try {
            // 1. Pre-initialization validation
            validateBeforeInit(descriptor);
            
            // 2. Prepare configuration
            NodeConfig preparedConfig = prepareConfiguration(descriptor, config);
            
            // 3. Call node's onLoad
            node.onLoad(descriptor, preparedConfig);
            
            // 4. Post-initialization verification
            verifyInitialization(node, descriptor);
            
            LOG.info("Node initialized successfully: {}", descriptor.getQualifiedId());
            
        } catch (NodeException e) {
            LOG.error("Node initialization failed: " + descriptor.getQualifiedId(), e);
            throw e;
        } catch (Exception e) {
            LOG.error("Unexpected error during node initialization: " + descriptor.getQualifiedId(), e);
            throw new NodeException("Failed to initialize node", e);
        }
    }
    
    /**
     * Validate node before initialization
     */
    private void validateBeforeInit(NodeDescriptor descriptor) throws NodeException {
        for (NodeValidator validator : validators) {
            var result = validator.validateDescriptor(descriptor);
            if (!result.valid()) {
                throw validator.toException(result);
            }
        }
    }
    
    /**
     * Prepare and enrich configuration
     */
    private NodeConfig prepareConfiguration(NodeDescriptor descriptor, NodeConfig config) {
        // Add default values from descriptor
        var enrichedProperties = new java.util.HashMap<>(config.properties());
        
        for (var property : descriptor.properties()) {
            if (!enrichedProperties.containsKey(property.name()) && property.defaultValue() != null) {
                enrichedProperties.put(property.name(), property.defaultValue());
            }
        }
        
        return new NodeConfig(
            config.nodeId(),
            config.instanceId(),
            enrichedProperties,
            config.runtimeSettings(),
            config.retryPolicy(),
            config.timeoutSettings()
        );
    }
    
    /**
     * Verify node is properly initialized
     */
    private void verifyInitialization(Node node, NodeDescriptor descriptor) throws NodeException {
        // Verify node has correct descriptor
        if (!descriptor.equals(node.getDescriptor())) {
            LOG.warn("Node descriptor mismatch after initialization");
        }
        
        // Additional verification checks can be added here
    }
}