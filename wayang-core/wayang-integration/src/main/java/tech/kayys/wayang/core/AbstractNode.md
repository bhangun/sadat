

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.UUID;

/**
 * Base abstract class for all nodes in the Wayang platform.
 * Provides common functionality for both AI and Integration nodes.
 */
@Slf4j
@Getter
@Setter
public abstract class AbstractNode {
    
    protected String nodeId;
    protected String nodeType;
    protected NodeConfig config;
    protected Map<String, Object> metadata;
    
    /**
     * Initialize the node with configuration
     */
    public void initialize(NodeConfig config) {
        this.nodeId = config.getId();
        this.nodeType = config.getType();
        this.config = config;
        this.metadata = config.getMetadata();
        onLoad();
    }
    
    /**
     * Lifecycle hook - called after configuration is loaded
     */
    protected void onLoad() {
        log.info("Node {} of type {} loaded", nodeId, nodeType);
    }
    
    /**
     * Main execution method - must be implemented by subclasses
     */
    public abstract ExecutionResult execute(NodeContext context);
    
    /**
     * Lifecycle hook - called before node is unloaded
     */
    protected void onUnload() {
        log.info("Node {} unloaded", nodeId);
    }
    
    /**
     * Validate node configuration
     */
    public ValidationResult validate() {
        ValidationResult result = new ValidationResult();
        
        if (nodeId == null || nodeId.isBlank()) {
            result.addError("Node ID is required");
        }
        
        if (nodeType == null || nodeType.isBlank()) {
            result.addError("Node type is required");
        }
        
        // Delegate to subclass for additional validation
        validateConfiguration(result);
        
        return result;
    }
    
    /**
     * Subclasses can override to add custom validation
     */
    protected void validateConfiguration(ValidationResult result) {
        // Default: no additional validation
    }
    
    /**
     * Get a configuration value
     */
    protected String getConfigValue(String path) {
        return config.getString(path);
    }
    
    /**
     * Get a configuration value with default
     */
    protected String getConfigValue(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }
    
    /**
     * Check if node supports streaming
     */
    public boolean supportsStreaming() {
        return false;
    }
    
    /**
     * Get node capabilities
     */
    public NodeCapabilities getCapabilities() {
        return NodeCapabilities.builder()
            .nodeId(nodeId)
            .nodeType(nodeType)
            .supportsStreaming(supportsStreaming())
            .supportsRetry(true)
            .build();
    }
}