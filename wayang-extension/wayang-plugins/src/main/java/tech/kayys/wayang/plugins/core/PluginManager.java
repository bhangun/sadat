
/**
 * Plugin manager - loads and manages plugins
 */
@ApplicationScoped
public class PluginManager {
    private final Map<String, Plugin> loadedPlugins = new ConcurrentHashMap<>();
    private final PluginLoader loader;
    private final PluginValidator validator;
    private final PluginRegistry registry;
    
    @Inject
    public PluginManager(PluginLoader loader, 
                        PluginValidator validator,
                        PluginRegistry registry) {
        this.loader = loader;
        this.validator = validator;
        this.registry = registry;
    }
    
    /**
     * Load plugin from artifact
     */
    public void loadPlugin(PluginArtifact artifact) throws PluginException {
        // Validate plugin
        ValidationResult validation = validator.validate(artifact);
        if (!validation.isValid()) {
            throw new PluginException("Plugin validation failed: " + validation.getErrors());
        }
        
        // Load plugin
        Plugin plugin = loader.load(artifact);
        
        // Initialize plugin
        PluginContext context = createPluginContext(plugin);
        plugin.initialize(context);
        
        // Register plugin
        loadedPlugins.put(plugin.getDescriptor().getId(), plugin);
        registry.register(plugin.getDescriptor());
    }
    
    /**
     * Unload plugin
     */
    public void unloadPlugin(String pluginId) {
        Plugin plugin = loadedPlugins.remove(pluginId);
        if (plugin != null) {
            plugin.shutdown();
            registry.unregister(pluginId);
        }
    }
    
    /**
     * Get loaded plugin
     */
    public Optional<Plugin> getPlugin(String pluginId) {
        return Optional.ofNullable(loadedPlugins.get(pluginId));
    }
}