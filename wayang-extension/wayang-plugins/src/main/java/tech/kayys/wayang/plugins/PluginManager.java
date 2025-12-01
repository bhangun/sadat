
public interface PluginManager {
    void registerPlugin(PluginDescriptor descriptor, InputStream artifact);
    void unregisterPlugin(String pluginId);
    Optional<PluginDescriptor> getPlugin(String pluginId);
    List<PluginDescriptor> listPlugins(PluginQuery query);
    void enablePlugin(String pluginId);
    void disablePlugin(String pluginId);
    PluginStatus getStatus(String pluginId);
}