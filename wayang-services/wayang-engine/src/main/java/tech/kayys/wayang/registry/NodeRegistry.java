package tech.kayys.wayang.registry;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for all node types (built-in and plugins)
 * Provides discovery, validation, and instantiation services
 */
@ApplicationScoped
@Startup
public class NodeRegistry {

    private final Map<String, NodeDescriptor> descriptors = new ConcurrentHashMap<>();
    private final Map<String, NodeFactory> factories = new ConcurrentHashMap<>();

    @Inject
    Instance<Node> builtinNodes;

    @Inject
    PluginLoader pluginLoader;

    @Inject
    SchemaValidator schemaValidator;

    public void initialize() {
        // Register built-in nodes
        registerBuiltinNodes();

        // Load plugin nodes
        loadPluginNodes();
    }

    /**
     * Register all built-in nodes using CDI discovery
     */
    private void registerBuiltinNodes() {
        builtinNodes.forEach(node -> {
            var annotation = node.getClass().getAnnotation(NodeType.class);
            if (annotation != null) {
                var nodeType = annotation.value();
                var descriptor = loadDescriptor(nodeType);
                var factory = new ReflectiveNodeFactory(node.getClass());

                register(nodeType, descriptor, factory);
            }
        });
    }

    /**
     * Load plugin nodes from plugin manager
     */
    private void loadPluginNodes() {
        var plugins = pluginLoader.loadAllPlugins();

        for (var plugin : plugins) {
            var descriptor = plugin.getDescriptor();
            var factory = plugin.getFactory();

            // Validate plugin descriptor
            var errors = schemaValidator.validate(descriptor);
            if (!errors.isEmpty()) {
                throw new PluginValidationException(
                        "Plugin descriptor validation failed: " + errors);
            }

            register(descriptor.getId(), descriptor, factory);
        }
    }

    /**
     * Register a node type with its descriptor and factory
     */
    public void register(String nodeType, NodeDescriptor descriptor, NodeFactory factory) {
        // Validate descriptor against schema
        validateDescriptor(descriptor);

        descriptors.put(nodeType, descriptor);
        factories.put(nodeType, factory);

        logger.info("Registered node type: {}", nodeType);
    }

    /**
     * Get node descriptor by type
     */
    public NodeDescriptor getDescriptor(String nodeType) {
        var descriptor = descriptors.get(nodeType);
        if (descriptor == null) {
            throw new NodeNotFoundException("Node type not found: " + nodeType);
        }
        return descriptor;
    }

    /**
     * Create node instance
     */
    public Node createNode(String nodeType) {
        var factory = factories.get(nodeType);
        if (factory == null) {
            throw new NodeNotFoundException("Node type not found: " + nodeType);
        }

        var descriptor = descriptors.get(nodeType);
        return factory.create(descriptor);
    }

    /**
     * List all available node types
     */
    public List<NodeDescriptor> listAll() {
        return new ArrayList<>(descriptors.values());
    }

    /**
     * List nodes by category
     */
    public List<NodeDescriptor> listByCategory(String category) {
        return descriptors.values().stream()
                .filter(d -> d.getType().startsWith(category))
                .collect(Collectors.toList());
    }

    /**
     * Validate descriptor against base schema
     */
    private void validateDescriptor(NodeDescriptor descriptor) {
        var errors = schemaValidator.validate(
                descriptor,
                "https://kayys.tech/schema/v1/node-base.schema.json");

        if (!errors.isEmpty()) {
            throw new DescriptorValidationException(
                    "Descriptor validation failed: " + errors);
        }
    }

    /**
     * Load descriptor from resources
     */
    private NodeDescriptor loadDescriptor(String nodeType) {
        var resourcePath = "/nodes/descriptors/" + nodeType + ".json";
        try {
            var json = Resources.readString(resourcePath);
            return JsonUtils.fromJson(json, NodeDescriptor.class);
        } catch (IOException e) {
            throw new DescriptorLoadException(
                    "Failed to load descriptor for: " + nodeType,
                    e);
        }
    }
}
