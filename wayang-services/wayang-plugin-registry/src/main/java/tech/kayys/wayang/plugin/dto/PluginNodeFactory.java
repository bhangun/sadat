package tech.kayys.wayang.api.plugin;

import tech.kayys.wayang.api.node.Node;

/**
 * Plugin factory for dynamically loaded nodes
 * Supports multiple loading strategies (classloader, WASM, container)
 */
public class PluginNodeFactory implements NodeFactory {

    private final PluginDescriptor pluginDescriptor;
    private final PluginLoader loader;

    public PluginNodeFactory(PluginDescriptor pluginDescriptor, PluginLoader loader) {
        this.pluginDescriptor = pluginDescriptor;
        this.loader = loader;
    }

    @Override
    public Node create(NodeDescriptor descriptor) {
        var implementation = pluginDescriptor.getImplementation();

        return switch (implementation.getType()) {
            case "maven", "jar" -> createJavaNode(implementation);
            case "wasm" -> createWasmNode(implementation);
            case "container" -> createContainerNode(implementation);
            case "python" -> createPythonNode(implementation);
            default -> throw new UnsupportedPluginTypeException(
                    "Unsupported plugin type: " + implementation.getType());
        };
    }

    private Node createJavaNode(PluginImplementation impl) {
        // Load class using isolated classloader
        var classLoader = loader.createIsolatedClassLoader(impl);
        try {
            var nodeClass = classLoader.loadClass(impl.getEntrypoint());
            return (Node) nodeClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new NodeInstantiationException("Failed to load Java plugin", e);
        }
    }

    private Node createWasmNode(PluginImplementation impl) {
        // Create WASM adapter node
        return new WasmNodeAdapter(impl, loader);
    }

    private Node createContainerNode(PluginImplementation impl) {
        // Create container sidecar adapter
        return new ContainerNodeAdapter(impl, loader);
    }

    private Node createPythonNode(PluginImplementation impl) {
        // Create Python subprocess adapter
        return new PythonNodeAdapter(impl, loader);
    }

    @Override
    public void validate(NodeDescriptor descriptor) {
        // Validate plugin implementation details
        var impl = pluginDescriptor.getImplementation();

        // Verify signature
        if (!loader.verifySignature(impl)) {
            throw new SecurityException("Plugin signature verification failed");
        }

        // Verify capabilities
        validateCapabilities(descriptor.getCapabilities());
    }

    private void validateCapabilities(List<String> capabilities) {
        // Check if required capabilities are allowed
        for (var capability : capabilities) {
            if (!isCapabilityAllowed(capability)) {
                throw new SecurityException(
                        "Capability not allowed: " + capability);
            }
        }
    }

    private boolean isCapabilityAllowed(String capability) {
        // Check against policy engine
        return PolicyEngine.instance().isCapabilityAllowed(capability);
    }
}
