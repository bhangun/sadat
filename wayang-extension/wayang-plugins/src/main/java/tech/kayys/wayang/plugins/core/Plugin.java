
/**
 * Plugin interface for extending the platform
 */
public interface Plugin {
    /**
     * Plugin metadata
     */
    PluginDescriptor getDescriptor();
    
    /**
     * Initialize plugin
     */
    void initialize(PluginContext context) throws PluginException;
    
    /**
     * Shutdown plugin
     */
    void shutdown();
    
    /**
     * Get plugin components
     */
    <T> Optional<T> getComponent(Class<T> componentType);
}