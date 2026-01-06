package tech.kayys.wayang.plugin.runtime;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public interface PluginManager {
    void registerPlugin(PluginDescriptor descriptor, InputStream artifact);
    void unregisterPlugin(String pluginId);
    Optional<PluginDescriptor> getPlugin(String pluginId);
    List<PluginDescriptor> listPlugins(PluginQuery query);
    void enablePlugin(String pluginId);
    void disablePlugin(String pluginId);
    PluginStatus getStatus(String pluginId);
}