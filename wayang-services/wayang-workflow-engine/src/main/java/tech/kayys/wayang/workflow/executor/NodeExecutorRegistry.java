package tech.kayys.wayang.workflow.executor;

import io.smallrye.mutiny.Uni;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import tech.kayys.wayang.workflow.exception.NodeNotFoundException;
import tech.kayys.wayang.workflow.model.NodeRegistration;
import tech.kayys.wayang.workflow.service.NodeContext;
import tech.kayys.wayang.workflow.service.PluginLoader;
import tech.kayys.wayang.common.spi.AbstractNode;
import tech.kayys.wayang.common.spi.Node;
import tech.kayys.wayang.schema.execution.ErrorPayload;
import tech.kayys.wayang.schema.node.NodeDefinition;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NodeExecutorRegistry - Central registry for node executors
 * 
 * Responsibilities:
 * - Register built-in and plugin node types
 * - Load node implementations on demand
 * - Manage node lifecycle (initialization, execution, cleanup)
 * - Provide node discovery and introspection
 * - Support hot-reload for plugin updates
 * 
 * Thread-safe and supports concurrent access
 */
@ApplicationScoped
public class NodeExecutorRegistry {

    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(NodeExecutorRegistry.class);

    @Inject
    PluginLoader pluginLoader;

    @Inject
    NodeFactory nodeFactory;

    // Registry of node type -> Node class
    private final Map<String, NodeRegistration> registrations = new ConcurrentHashMap<>();

    // Cache of instantiated nodes (for reuse if stateless)
    private final Map<String, Node> nodeCache = new ConcurrentHashMap<>();

    /**
     * Initialize with built-in nodes
     */
    @PostConstruct
    void initialize() {
        LOG.info("Initializing Node Executor Registry");
        registerBuiltInNodes();
    }

    /**
     * Register built-in node types
     */
    private void registerBuiltInNodes() {
        // Core nodes
        /*
         * register("builtin.start", StartNode.class);
         * register("builtin.end", EndNode.class);
         */

        // Control flow nodes
        /*
         * register("builtin.decision", DecisionNode.class);
         * register("builtin.parallel", ParallelNode.class);
         * register("builtin.loop", LoopNode.class);
         * register("builtin.switch", SwitchNode.class);
         */

        // Integration nodes
        /*
         * register("builtin.http", HttpNode.class);
         * register("builtin.transform", TransformNode.class);
         * register("builtin.script", ScriptNode.class);
         */

        // AI/Agent nodes
        // register("builtin.llm", LLMNode.class);
        // register("builtin.rag", RAGNode.class);
        // register("builtin.agent", AgentNode.class);

        // System nodes
        // register("builtin.error_handler", ErrorHandlerNode.class);
        // register("builtin.human_decision", HumanDecisionNode.class);
        // register("builtin.self_healing", SelfHealingNode.class);
        // register("builtin.audit", AuditNode.class);

        LOG.infof("Registered %d built-in node types", registrations.size());
    }

    /**
     * Register a node type
     */
    public void register(String nodeType, Class<? extends Node> nodeClass) {
        register(nodeType, nodeClass, NodeRegistration.RegistrationType.BUILT_IN);
    }

    /**
     * Register a custom NodeExecutor directly
     */
    public void registerExecutor(String nodeType, NodeExecutor executor) {
        NodeRegistration registration = NodeRegistration.builder()
                .nodeType(nodeType)
                .nodeClass((Class<? extends Node>) executor.getClass())
                .registrationType(NodeRegistration.RegistrationType.CUSTOM)
                .registeredAt(java.time.Instant.now())
                .version("1.0.0")
                .build();

        registrations.put(nodeType, registration);
        LOG.infof("Registered custom executor for node type: %s", nodeType);
    }

    /**
     * Register a node type with metadata
     */
    public void register(
            String nodeType,
            Class<? extends Node> nodeClass,
            NodeRegistration.RegistrationType registrationType) {

        NodeRegistration registration = NodeRegistration.builder()
                .nodeType(nodeType)
                .nodeClass(nodeClass)
                .registrationType(registrationType)
                .registeredAt(java.time.Instant.now())
                .version("1.0.0")
                .build();

        registrations.put(nodeType, registration);
        LOG.infof("Registered node type: %s -> %s", nodeType, nodeClass.getSimpleName());
    }

    /**
     * Register a plugin node
     */
    public Uni<Void> registerPlugin(
            String nodeType,
            String pluginId,
            String version) {

        return pluginLoader.loadNodeClass(pluginId, nodeType)
                .invoke(nodeClass -> {
                    NodeRegistration registration = NodeRegistration.builder()
                            .nodeType(nodeType)
                            .nodeClass(nodeClass)
                            .registrationType(NodeRegistration.RegistrationType.PLUGIN)
                            .pluginId(pluginId)
                            .version(version)
                            .registeredAt(java.time.Instant.now())
                            .build();

                    registrations.put(nodeType, registration);
                    LOG.infof("Registered plugin node: %s from plugin: %s",
                            nodeType, pluginId);
                })
                .replaceWithVoid();
    }

    public NodeExecutor getExecutor(String nodeType) {
        // Check if we have a specific executor for this node type
        NodeExecutor executor = findSpecificExecutor(nodeType);
        if (executor != null) {
            return executor;
        }

        // Fall back to the generic approach if no specific executor found
        return new GenericNodeExecutor(nodeType);
    }

    /**
     * Find a specific executor for a node type
     */
    private NodeExecutor findSpecificExecutor(String nodeType) {
        // Check if we have a registered executor for this type
        NodeRegistration registration = registrations.get(nodeType);
        if (registration != null) {
            // Create and return the appropriate executor based on registration
            try {
                Class<? extends Node> nodeClass = (Class<? extends Node>) registration.getNodeClass();
                Node nodeInstance = nodeFactory.create(nodeClass).await().indefinitely();
                if (nodeInstance instanceof NodeExecutor) {
                    return (NodeExecutor) nodeInstance;
                }
            } catch (Exception e) {
                LOG.warnf("Could not create executor for node type %s: %s", nodeType, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Generic executor that can handle any node type through the registry
     */
    private class GenericNodeExecutor implements NodeExecutor {
        private final String nodeType;

        public GenericNodeExecutor(String nodeType) {
            this.nodeType = nodeType;
        }

        @Override
        public Uni<NodeExecutionResult> execute(NodeDefinition nodeDef, NodeContext context) {
            return getNode(nodeType).flatMap(node -> {
                if (node instanceof NodeExecutor) {
                    return ((NodeExecutor) node).execute(nodeDef, context);
                } else {
                    // If the node is not a NodeExecutor, return an error
                    return Uni.createFrom().item(
                            NodeExecutionResult.error(nodeDef.getId(),
                                    ErrorPayload.builder()
                                            .type(ErrorPayload.ErrorType.EXECUTION_ERROR)
                                            .message("Node does not implement NodeExecutor interface: "
                                                    + nodeDef.getType())
                                            .build()));
                }
            });
        }

        @Override
        public String getNodeType() {
            return nodeType;
        }

        @Override
        public boolean canHandle(String nodeType) {
            return this.nodeType.equals(nodeType);
        }
    }

    /**
     * Get node instance by type
     */
    public Uni<Node> getNode(String nodeType) {
        NodeRegistration registration = registrations.get(nodeType);

        if (registration == null) {
            return Uni.createFrom().failure(
                    new NodeNotFoundException("Node type not found: " + nodeType));
        }

        // Check cache first (for stateless nodes)
        Node cached = nodeCache.get(nodeType);
        if (cached != null && isStateless(cached)) {
            return Uni.createFrom().item(cached);
        }

        // Create new instance
        return createNode(registration)
                .invoke(node -> {
                    if (isStateless(node)) {
                        nodeCache.put(nodeType, node);
                    }
                });
    }

    /**
     * Create node instance
     */
    private Uni<Node> createNode(NodeRegistration registration) {
        if (registration.getRegistrationType() == NodeRegistration.RegistrationType.PLUGIN) {
            return pluginLoader.loadNode(
                    registration.getPluginId(),
                    registration.getNodeType());
        }

        return nodeFactory.create(registration.getNodeClass());
    }

    /**
     * Check if node is stateless
     */
    private boolean isStateless(Node node) {
        if (node instanceof AbstractNode) {
            return ((AbstractNode) node).isStateless();
        }
        return false;
    }

    /**
     * Get node registration info
     */
    public NodeRegistration getRegistration(String nodeType) {
        return registrations.get(nodeType);
    }

    /**
     * List all registered node types
     */
    public Map<String, NodeRegistration> listAll() {
        return new java.util.HashMap<>(registrations);
    }

    /**
     * List built-in nodes
     */
    public Map<String, NodeRegistration> listBuiltIn() {
        return registrations.entrySet().stream()
                .filter(e -> e.getValue().getRegistrationType() == NodeRegistration.RegistrationType.BUILT_IN)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue));
    }

    /**
     * List plugin nodes
     */
    public Map<String, NodeRegistration> listPlugins() {
        return registrations.entrySet().stream()
                .filter(e -> e.getValue().getRegistrationType() == NodeRegistration.RegistrationType.PLUGIN)
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue));
    }

    /**
     * Unregister node type
     */
    public void unregister(String nodeType) {
        NodeRegistration removed = registrations.remove(nodeType);
        nodeCache.remove(nodeType);

        if (removed != null) {
            LOG.infof("Unregistered node type: %s", nodeType);
        }
    }

    /**
     * Reload node (for hot-reload)
     */
    public Uni<Void> reload(String nodeType) {
        nodeCache.remove(nodeType);
        NodeRegistration registration = registrations.get(nodeType);

        if (registration == null) {
            return Uni.createFrom().failure(
                    new NodeNotFoundException("Node type not found: " + nodeType));
        }

        if (registration.getRegistrationType() == NodeRegistration.RegistrationType.PLUGIN) {
            return pluginLoader.reloadPlugin(registration.getPluginId())
                    .replaceWithVoid();
        }

        return Uni.createFrom().voidItem();
    }

    /**
     * Clear cache (useful for testing or hot-reload)
     */
    public void clearCache() {
        nodeCache.clear();
        LOG.info("Node cache cleared");
    }

    /**
     * Get statistics
     */
    public RegistryStatistics getStatistics() {
        long builtInCount = registrations.values().stream()
                .filter(r -> r.getRegistrationType() == NodeRegistration.RegistrationType.BUILT_IN)
                .count();

        long pluginCount = registrations.values().stream()
                .filter(r -> r.getRegistrationType() == NodeRegistration.RegistrationType.PLUGIN)
                .count();

        return RegistryStatistics.builder()
                .totalNodes(registrations.size())
                .builtInNodes(builtInCount)
                .pluginNodes(pluginCount)
                .cachedNodes(nodeCache.size())
                .build();
    }
}
