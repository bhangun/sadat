package tech.kayys.wayang.node.core.factory;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.node.core.Node;
import tech.kayys.wayang.node.core.exception.NodeFactoryException;
import tech.kayys.wayang.node.core.model.ImplementationType;
import tech.kayys.wayang.node.core.model.NodeDescriptor;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default factory for creating nodes from Maven artifacts.
 * 
 * Handles:
 * - ClassLoader isolation
 * - JAR loading from coordinates
 * - Reflection-based instantiation
 * - Class caching
 */
@ApplicationScoped
public class DefaultNodeFactory implements NodeFactory {
    
    private static final Logger LOG = LoggerFactory.getLogger(DefaultNodeFactory.class);
    
    private final ConcurrentMap<String, ClassLoader> classLoaderCache;
    private final ConcurrentMap<String, Class<? extends Node>> classCache;
    
    public DefaultNodeFactory() {
        this.classLoaderCache = new ConcurrentHashMap<>();
        this.classCache = new ConcurrentHashMap<>();
    }
    
    @Override
    public Node create(NodeDescriptor descriptor) throws NodeFactoryException {
        try {
            // Get or create class loader
            ClassLoader classLoader = getOrCreateClassLoader(descriptor);
            
            // Load node class
            Class<? extends Node> nodeClass = loadNodeClass(descriptor, classLoader);
            
            // Instantiate node
            Node node = instantiateNode(nodeClass);
            
            LOG.info("Created node instance: {}", descriptor.getQualifiedId());
            
            return node;
            
        } catch (Exception e) {
            throw new NodeFactoryException(
                "Failed to create node: " + descriptor.getQualifiedId(), 
                e
            );
        }
    }
    
    @Override
    public void validate(NodeDescriptor descriptor) throws NodeFactoryException {
        if (descriptor.implementation().type() != ImplementationType.MAVEN) {
            throw new NodeFactoryException(
                "Descriptor implementation type must be MAVEN, got: " + 
                descriptor.implementation().type()
            );
        }
        
        String coordinate = descriptor.implementation().coordinate();
        if (coordinate == null || coordinate.isBlank()) {
            throw new NodeFactoryException(
                "Maven coordinate cannot be null or empty"
            );
        }
        
        // Validate coordinate format (groupId:artifactId:version)
        String[] parts = coordinate.split(":");
        if (parts.length != 3) {
            throw new NodeFactoryException(
                "Invalid Maven coordinate format. Expected groupId:artifactId:version, got: " 
                + coordinate
            );
        }
    }
    
    @Override
    public boolean supports(NodeDescriptor descriptor) {
        return descriptor.implementation().type() == ImplementationType.MAVEN;
    }
    
    @Override
    public ImplementationType getImplementationType() {
        return ImplementationType.MAVEN;
    }
    
    /**
     * Get or create isolated ClassLoader for the node
     */
    private ClassLoader getOrCreateClassLoader(NodeDescriptor descriptor) 
            throws NodeFactoryException {
        
        String coordinate = descriptor.implementation().coordinate();
        
        return classLoaderCache.computeIfAbsent(coordinate, key -> {
            try {
                // In production, this would resolve artifacts from Maven repo
                // For now, we use the parent classloader
                URL[] urls = resolveArtifactUrls(coordinate);
                return new URLClassLoader(
                    urls,
                    Thread.currentThread().getContextClassLoader()
                );
            } catch (Exception e) {
                throw new RuntimeException(
                    "Failed to create ClassLoader for: " + coordinate, 
                    e
                );
            }
        });
    }
    
    /**
     * Resolve Maven artifact URLs
     * 
     * In production, this would:
     * 1. Parse coordinate
     * 2. Query artifact repository
     * 3. Download JAR if needed
     * 4. Return file:// URLs
     */
    private URL[] resolveArtifactUrls(String coordinate) throws Exception {
        // Simplified implementation
        // Real implementation would use Aether/Maven Resolver
        LOG.warn("Using simplified artifact resolution for: {}", coordinate);
        return new URL[0];
    }
    
    /**
     * Load the Node implementation class
     */
    @SuppressWarnings("unchecked")
    private Class<? extends Node> loadNodeClass(
        NodeDescriptor descriptor, 
        ClassLoader classLoader
    ) throws NodeFactoryException {
        
        String cacheKey = descriptor.getQualifiedId();
        
        return classCache.computeIfAbsent(cacheKey, key -> {
            try {
                // Get main class name from descriptor metadata
                String className = (String) descriptor.metadata().get("mainClass");
                if (className == null) {
                    throw new NodeFactoryException(
                        "Node descriptor missing 'mainClass' in metadata"
                    );
                }
                
                // Load class
                Class<?> clazz = classLoader.loadClass(className);
                
                // Verify it implements Node
                if (!Node.class.isAssignableFrom(clazz)) {
                    throw new NodeFactoryException(
                        "Class does not implement Node interface: " + className
                    );
                }
                
                return (Class<? extends Node>) clazz;
                
            } catch (ClassNotFoundException e) {
                throw new NodeFactoryException(
                    "Node class not found: " + descriptor.getQualifiedId(), 
                    e
                );
            }
        });
    }
    
    /**
     * Instantiate the node using reflection
     */
    private Node instantiateNode(Class<? extends Node> nodeClass) 
            throws NodeFactoryException {
        try {
            // Try no-arg constructor first
            Constructor<? extends Node> constructor = nodeClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
            
        } catch (NoSuchMethodException e) {
            throw new NodeFactoryException(
                "Node class must have a no-arg constructor: " + nodeClass.getName(),
                e
            );
        } catch (Exception e) {
            throw new NodeFactoryException(
                "Failed to instantiate node: " + nodeClass.getName(),
                e
            );
        }
    }
    
    /**
     * Clear all caches
     */
    public void clearCaches() {
        classLoaderCache.clear();
        classCache.clear();
        LOG.info("Cleared DefaultNodeFactory caches");
    }
}