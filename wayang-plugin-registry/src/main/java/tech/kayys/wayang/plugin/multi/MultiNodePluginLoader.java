package tech.kayys.wayang.plugin.multi;

import java.util.HashSet;
import java.util.Map;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.plugin.CommunicationProtocol;
import tech.kayys.wayang.plugin.ControlPlaneExecutorRegistry;
import tech.kayys.wayang.plugin.ControlPlaneNodeRegistry;
import tech.kayys.wayang.plugin.LoadedPlugin;
import tech.kayys.wayang.plugin.PluginRegistration;
import tech.kayys.wayang.plugin.PluginResourceLoader;
import tech.kayys.wayang.plugin.SchemaReference;
import tech.kayys.wayang.plugin.SchemaValidator;
import tech.kayys.wayang.plugin.SharedResources;
import tech.kayys.wayang.plugin.UIReference;
import tech.kayys.wayang.plugin.execution.ExecutionMode;
import tech.kayys.wayang.plugin.executor.ExecutorBinding;
import tech.kayys.wayang.plugin.executor.ExecutorManifest;
import tech.kayys.wayang.plugin.executor.ExecutorRegistration;
import tech.kayys.wayang.plugin.node.NodeDefinition;
import tech.kayys.wayang.plugin.node.NodeManifest;

/**
 * Loads and registers plugins with multiple nodes
 */
@ApplicationScoped
public class MultiNodePluginLoader {

    private static final Logger LOG = Logger.getLogger(MultiNodePluginLoader.class);

    @Inject
    ControlPlaneNodeRegistry nodeRegistry;

    @Inject
    ControlPlaneExecutorRegistry executorRegistry;

    @Inject
    SchemaValidator schemaValidator;

    @Inject
    PluginResourceLoader resourceLoader;

    /**
     * Load plugin with multiple nodes
     */
    public Uni<PluginRegistration> loadPlugin(
            MultiNodePluginManifest manifest,
            LoadedPlugin loadedPlugin) {

        LOG.infof("Loading multi-node plugin: %s with %d nodes",
                manifest.name, manifest.nodes.size());

        return Uni.createFrom().item(() -> {

            PluginRegistration registration = new PluginRegistration();
            registration.pluginId = manifest.pluginId;
            registration.pluginName = manifest.name;
            registration.version = manifest.version;
            registration.family = manifest.family;
            registration.registeredAt = java.time.Instant.now();

            // 1. Load shared resources
            loadSharedResources(manifest.shared, loadedPlugin);

            // 2. Register executors
            for (ExecutorManifest executorManifest : manifest.executors) {
                registerExecutor(executorManifest, loadedPlugin, registration);
            }

            // 3. Register all nodes
            for (NodeManifest nodeManifest : manifest.nodes) {
                registerNode(nodeManifest, manifest, loadedPlugin, registration);
            }

            LOG.infof("Successfully registered plugin %s with %d nodes and %d executors",
                    manifest.pluginId,
                    registration.registeredNodes.size(),
                    registration.registeredExecutors.size());

            return registration;
        });
    }

    /**
     * Load shared resources (schemas, widgets, etc.)
     */
    private void loadSharedResources(SharedResources shared, LoadedPlugin loadedPlugin) {
        // Load shared schemas
        for (Map.Entry<String, String> entry : shared.schemas.entrySet()) {
            String schemaName = entry.getKey();
            String schemaPath = entry.getValue();

            byte[] schemaContent = loadedPlugin.resources.get(schemaPath);
            if (schemaContent != null) {
                // Cache schema for reuse
                resourceLoader.cacheSchema(schemaName, new String(schemaContent));
                LOG.debugf("Loaded shared schema: %s", schemaName);
            }
        }

        // Load shared widgets
        for (Map.Entry<String, String> entry : shared.widgets.entrySet()) {
            String widgetName = entry.getKey();
            String widgetPath = entry.getValue();

            byte[] widgetContent = loadedPlugin.resources.get(widgetPath);
            if (widgetContent != null) {
                resourceLoader.cacheWidget(widgetName, widgetContent);
                LOG.debugf("Loaded shared widget: %s", widgetName);
            }
        }
    }

    /**
     * Register executor
     */
    private void registerExecutor(
            ExecutorManifest executorManifest,
            LoadedPlugin loadedPlugin,
            PluginRegistration registration) {

        LOG.infof("Registering executor: %s for nodes: %s",
                executorManifest.executorId, executorManifest.nodeTypes);

        ExecutorRegistration executorReg = new ExecutorRegistration();
        executorReg.executorId = executorManifest.executorId;
        executorReg.executorType = "plugin";
        executorReg.protocol = CommunicationProtocol.GRPC; // Default
        executorReg.inProcess = executorManifest.inProcess;
        executorReg.supportedNodes = new HashSet<>(executorManifest.nodeTypes);

        // Get executor instance from loaded plugin
        if (executorManifest.inProcess && executorManifest.className != null) {
            Object executorInstance = loadedPlugin.instances.get(executorManifest.className);
            if (executorInstance != null) {
                // Store for in-process execution
                executorRegistry.registerInProcessExecutor(
                        executorManifest.executorId, executorInstance);
            }
        }

        executorRegistry.register(executorReg).await().indefinitely();
        registration.registeredExecutors.add(executorManifest.executorId);
    }

    /**
     * Register node definition
     */
    private void registerNode(
            NodeManifest nodeManifest,
            MultiNodePluginManifest pluginManifest,
            LoadedPlugin loadedPlugin,
            PluginRegistration registration) {

        LOG.infof("Registering node: %s (%s)",
                nodeManifest.type, nodeManifest.label);

        // Create node definition
        NodeDefinition nodeDef = new NodeDefinition();

        nodeDef.type = nodeManifest.type;
        nodeDef.label = nodeManifest.label;
        nodeDef.category = nodeManifest.category != null ? nodeManifest.category : pluginManifest.family;
        nodeDef.subCategory = nodeManifest.subCategory;
        nodeDef.description = nodeManifest.description;
        nodeDef.version = pluginManifest.version;
        nodeDef.author = pluginManifest.author;

        // Load schemas
        nodeDef.configSchema = loadSchema(nodeManifest.configSchema, loadedPlugin);
        nodeDef.inputSchema = loadSchema(nodeManifest.inputSchema, loadedPlugin);
        nodeDef.outputSchema = loadSchema(nodeManifest.outputSchema, loadedPlugin);

        // Executor binding
        String executorId = nodeManifest.executorId != null ? nodeManifest.executorId
                : getDefaultExecutorId(pluginManifest, nodeManifest);

        nodeDef.executorBinding = new ExecutorBinding(
                executorId,
                ExecutionMode.SYNC,
                CommunicationProtocol.GRPC);

        // UI reference
        if (nodeManifest.widgetId != null) {
            nodeDef.uiReference = new UIReference(nodeManifest.widgetId);
        }

        // Register
        nodeRegistry.register(nodeDef);
        registration.registeredNodes.add(nodeManifest.type);
    }

    /**
     * Load schema from reference
     */
    private com.networknt.schema.JsonSchema loadSchema(
            SchemaReference schemaRef,
            LoadedPlugin loadedPlugin) {

        if (schemaRef == null || schemaRef.content == null) {
            return null;
        }

        String schemaJson = switch (schemaRef.type) {
            case INLINE -> schemaRef.content;
            case FILE -> {
                byte[] content = loadedPlugin.resources.get(schemaRef.content);
                yield content != null ? new String(content) : null;
            }
            case URL -> resourceLoader.loadFromUrl(schemaRef.content);
        };

        if (schemaJson != null) {
            return schemaValidator.createSchema(schemaJson);
        }

        return null;
    }

    /**
     * Get default executor ID for node
     */
    private String getDefaultExecutorId(
            MultiNodePluginManifest plugin,
            NodeManifest node) {

        // If plugin has only one executor, use it
        if (plugin.executors.size() == 1) {
            return plugin.executors.get(0).executorId;
        }

        // Otherwise, construct from plugin ID and node type
        return plugin.pluginId + ".executor." + node.type.replace(".", "-");
    }
}
