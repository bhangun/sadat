



import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration container for nodes.
 * Wraps JSON configuration and provides convenience methods.
 */
@Data
public class NodeConfig {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private String id;
    private String type;
    private JsonNode config;
    private Map<String, Object> metadata = new HashMap<>();
    
    /**
     * Get string value from config path
     */
    public String getString(String path) {
        return getString(path, null);
    }
    
    /**
     * Get string value with default
     */
    public String getString(String path, String defaultValue) {
        JsonNode node = getNode(path);
        return node != null && !node.isNull() ? node.asText() : defaultValue;
    }
    
    /**
     * Get integer value
     */
    public Integer getInteger(String path) {
        return getInteger(path, null);
    }
    
    /**
     * Get integer value with default
     */
    public Integer getInteger(String path, Integer defaultValue) {
        JsonNode node = getNode(path);
        return node != null && !node.isNull() ? node.asInt() : defaultValue;
    }
    
    /**
     * Get boolean value
     */
    public Boolean getBoolean(String path) {
        return getBoolean(path, null);
    }
    
    /**
     * Get boolean value with default
     */
    public Boolean getBoolean(String path, Boolean defaultValue) {
        JsonNode node = getNode(path);
        return node != null && !node.isNull() ? node.asBoolean() : defaultValue;
    }
    
    /**
     * Get node at path
     */
    private JsonNode getNode(String path) {
        if (config == null) {
            return null;
        }
        
        String[] parts = path.split("\\.");
        JsonNode current = config;
        
        for (String part : parts) {
            if (current == null || current.isNull()) {
                return null;
            }
            current = current.get(part);
        }
        
        return current;
    }
    
    /**
     * Get config as specific type
     */
    public <T> T getConfigAs(Class<T> type) {
        return mapper.convertValue(config, type);
    }
}