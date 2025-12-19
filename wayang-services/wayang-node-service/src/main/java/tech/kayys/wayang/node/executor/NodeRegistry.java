package tech.kayys.wayang.node.executor;

import io.smallrye.mutiny.Uni;
import io.quarkus.hibernate.reactive.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.*;
import org.jboss.logging.Logger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NodeRegistry - Central registry for all node types and plugins.
 * 
 * Responsibilities:
 * - Register and discover node types (built-in and plugins)
 * - Load node implementations dynamically
 * - Manage node descriptors and metadata
 * - Support hot-reload for plugin updates
 * - Version management and compatibility checking
 * 
 * Design Principles:
 * - Lazy loading of node implementations
 * - Thread-safe registration and lookup
 * - Support for multiple versions
 * - Sandbox isolation for untrusted plugins
 * - Efficient caching with invalidation
 */
@ApplicationScoped
public class NodeRegistry {

    private static final Logger LOG = Logger.getLogger(NodeRegistry.class);

    @Inject
    NodeMetadataRepository metadataRepository;

    @Inject
    PluginLoader pluginLoader;

    @Inject
    NodeFactory nodeFactory;

    // Cache of loaded descriptors
    private final Map<String, NodeDescriptor> descriptorCache = new ConcurrentHashMap<>();

    // Registry of node constructors
    private final Map<String, Class<? extends Node>> nodeClasses = new ConcurrentHashMap<>();

    /**
     * Register a built-in node type.
     */
    public void registerBuiltIn(String nodeType, Class<? extends Node> nodeClass) {
        LOG.infof("Registering built-in node type: %s", nodeType);
        nodeClasses.put(nodeType, nodeClass);
    }

    /**
     * Register a plugin-based node type.
     */
    public Uni<Void> registerPlugin(PluginDescriptor plugin) {
        LOG.infof("Registering plugin: %s@%s", plugin.getId(), plugin.getVersion());

        return Uni.createFrom().deferred(() -> {
            // Validate plugin
            if (!validatePlugin(plugin)) {
                return Uni.createFrom().failure(
                        new PluginValidationException("Plugin validation failed: " + plugin.getId()));
            }

            // Convert to node metadata
            NodeMetadata metadata = NodeMetadata.fromPlugin(plugin);

            // Persist metadata
            return metadataRepository.persist(metadata)
                    .onItem().invoke(() -> {
                        // Cache descriptor
                        NodeDescriptor descriptor = NodeDescriptor.fromMetadata(metadata);
                        descriptorCache.put(plugin.getId(), descriptor);

                        LOG.infof("Successfully registered plugin: %s", plugin.getId());
                    })
                    .replaceWithVoid();
        });
    }

    /**
     * Get node descriptor by type.
     */
    public NodeDescriptor getDescriptor(String nodeType) {
        // Check cache first
        NodeDescriptor cached = descriptorCache.get(nodeType);
        if (cached != null) {
            return cached;
        }

        // Load from database
        NodeMetadata metadata = metadataRepository.findByNodeType(nodeType)
                .await().indefinitely();

        if (metadata != null) {
            NodeDescriptor descriptor = NodeDescriptor.fromMetadata(metadata);
            descriptorCache.put(nodeType, descriptor);
            return descriptor;
        }

        throw new NodeNotFoundException("Node type not found: " + nodeType);
    }

    /**
     * Load node instance.
     */
    public Uni<Node> load(String nodeType) {
        return Uni.createFrom().deferred(() -> {
            // Check if built-in
            Class<? extends Node> nodeClass = nodeClasses.get(nodeType);
            if (nodeClass != null) {
                return nodeFactory.create(nodeClass);
            }

            // Load from plugin
            NodeDescriptor descriptor = getDescriptor(nodeType);
            if (descriptor.getPluginRef() != null) {
                return pluginLoader.loadNode(descriptor.getPluginRef(), nodeType);
            }

            return Uni.createFrom().failure(
                    new NodeNotFoundException("Cannot load node: " + nodeType));
        });
    }

    /**
     * List all available node types.
     */
    public Uni<List<NodeDescriptor>> listAvailable() {
        return metadataRepository.listAll()
                .map(metadataList -> metadataList.stream()
                        .map(NodeDescriptor::fromMetadata)
                        .toList());
    }

    /**
     * Search nodes by capability.
     */
    public Uni<List<NodeDescriptor>> findByCapability(String capability) {
        return metadataRepository.findByCapability(capability)
                .map(metadataList -> metadataList.stream()
                        .map(NodeDescriptor::fromMetadata)
                        .toList());
    }

    /**
     * Unregister node type.
     */
    public Uni<Void> unregister(String nodeType) {
        descriptorCache.remove(nodeType);
        nodeClasses.remove(nodeType);
        return metadataRepository.deleteByNodeType(nodeType);
    }

    /**
     * Clear cache (for hot-reload).
     */
    public void clearCache() {
        descriptorCache.clear();
        LOG.info("Node descriptor cache cleared");
    }

    private boolean validatePlugin(PluginDescriptor plugin) {
        // Validate required fields
        if (plugin.getId() == null || plugin.getName() == null) {
            return false;
        }

        // Validate version format
        if (!isValidVersion(plugin.getVersion())) {
            return false;
        }

        // Validate implementation
        if (plugin.getImplementation() == null) {
            return false;
        }

        return true;
    }

    private boolean isValidVersion(String version) {
        return version != null && version.matches("\\d+\\.\\d+\\.\\d+.*");
    }
}
