package tech.kayys.wayang.plugin.runtime.model;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Custom ClassLoader for plugin isolation
 */
class PluginClassLoader extends URLClassLoader {
    
    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) 
            throws ClassNotFoundException {
        
        // Implement parent-last classloading for isolation
        synchronized (getClassLoadingLock(name)) {
            // Check if already loaded
            Class<?> c = findLoadedClass(name);
            
            if (c == null) {
                // Try to load from plugin first (parent-last)
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException e) {
                    // Fall back to parent
                    c = super.loadClass(name, resolve);
                }
            }
            
            if (resolve) {
                resolveClass(c);
            }
            
            return c;
        }
    }
}
