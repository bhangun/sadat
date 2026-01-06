
// Plugin Loader with Isolation

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.Path.Node;
import jakarta.ws.rs.Path;

@ApplicationScoped
public class IsolatedPluginLoader {
    private final Map<String, PluginContext> loadedPlugins = new ConcurrentHashMap<>();
    
    public void load(String artifactId, PluginDescriptor descriptor) {
        // Create isolated classloader
        ClassLoader pluginClassLoader = createIsolatedClassLoader(artifactId);
        
        // Load plugin classes
        List<Class<? extends Node>> nodeClasses = loadNodeClasses(
            pluginClassLoader,
            descriptor
        );
        
        // Create plugin context
        PluginContext context = PluginContext.builder()
            .pluginId(descriptor.getId())
            .classLoader(pluginClassLoader)
            .nodeClasses(nodeClasses)
            .descriptor(descriptor)
            .build();
        
        loadedPlugins.put(descriptor.getId(), context);
    }
    
    public void unload(String pluginId) {
        PluginContext context = loadedPlugins.remove(pluginId);
        if (context != null) {
            // Close classloader
            if (context.getClassLoader() instanceof Closeable) {
                try {
                    ((Closeable) context.getClassLoader()).close();
                } catch (IOException e) {
                    logger.warn("Failed to close classloader for plugin: " + pluginId, e);
                }
            }
        }
    }
    
    private ClassLoader createIsolatedClassLoader(String artifactId) {
        // Load artifact
        Path artifactPath = artifactStore.getPath(artifactId);
        
        try {
            URL artifactUrl = artifactPath.toUri().toURL();
            return new URLClassLoader(
                new URL[]{artifactUrl},
                getParentClassLoader()
            );
        } catch (MalformedURLException e) {
            throw new PluginLoadException("Failed to create classloader", e);
        }
    }
}