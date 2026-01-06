

package io.wayang.plugin.loader;

import io.smallrye.mutiny.Uni;
import io.wayang.plugin.common.ErrorPayload;
import io.wayang.plugin.common.ExecutionResult;
import io.wayang.plugin.spi.Node;
import io.wayang.plugin.spi.NodeContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plugin Loader Service - Manages plugin lifecycle and isolation
 * 
 * Responsibilities:
 * - Load plugins from artifact repository
 * - Manage plugin isolation (classloader/WASM/container)
 * - Verify signatures before loading
 * - Handle hot-reload and unload
 * - Provide error-as-input semantics
 * - Track loaded instances
 * 
 * Error Handling Strategy (from Blueprint):
 * - Every plugin execution has success and error outputs
 * - Errors are structured ErrorPayload objects
 * - Supports retry, fallback, and HITL escalation
 * - Circuit breaker for failing plugins
 */

@ApplicationScoped
public class PluginLoaderService {

    private static final Logger LOG = Logger.getLogger(PluginLoaderService.class);

    @Inject
    PluginRegistryClient registryClient;

    @Inject
    ArtifactStorageClient artifactClient;

    @Inject
    PluginSecurityService securityService;

    @Inject
    PluginAuditService auditService;

    @Inject
    PluginIsolationManager isolationManager;

    @Inject
    ErrorHandlerService errorHandler;

    // Cache of loaded plugins
    private final Map<PluginKey, LoadedPlugin> loadedPlugins = new ConcurrentHashMap<>();

    /**
     * Load plugin by ID and version
     * Returns ExecutionResult with success or error output
     */
    public Uni<ExecutionResult<LoadedPlugin>> loadPlugin(
            String pluginId, 
            String version,
            LoadOptions options) {
        
        LOG.infof("Loading plugin: %s version %s", pluginId, version);

        PluginKey key = new PluginKey(pluginId, version);

        // Check if already loaded
        LoadedPlugin existing = loadedPlugins.get(key);
        if (existing != null && !options.isForceReload()) {
            return Uni.createFrom().item(ExecutionResult.success(existing));
        }

        return registryClient.getPlugin(pluginId, version)
            .onItem().transformToUni(descriptor -> {
                // Verify plugin is approved
                if (!descriptor.getStatus().equals("approved")) {
                    ErrorPayload error = ErrorPayload.builder()
                        .type("SecurityError")
                        .message("Plugin not approved: " + descriptor.getStatus())
                        .retryable(false)
                        .originNode("PluginLoader")
                        .timestamp(Instant.now())
                        .suggestedAction("escalate")
                        .build();
                    
                    return Uni.createFrom().item(ExecutionResult.error(error));
                }

                // Verify signature
                return securityService.verifySignature(descriptor)
                    .onItem().transformToUni(verified -> {
                        if (!verified) {
                            ErrorPayload error = ErrorPayload.builder()
                                .type("SecurityError")
                                .message("Signature verification failed")
                                .retryable(false)
                                .originNode("PluginLoader")
                                .timestamp(Instant.now())
                                .suggestedAction("abort")
                                .build();
                            
                            return Uni.createFrom().item(ExecutionResult.error(error));
                        }

                        // Download artifact
                        return artifactClient.downloadArtifact(descriptor.getImplementation())
                            .onItem().transformToUni(artifactData -> 
                                loadPluginFromArtifact(descriptor, artifactData, key)
                            );
                    });
            })
            .onFailure().recoverWithItem(throwable -> {
                LOG.error("Failed to load plugin", throwable);
                
                ErrorPayload error = ErrorPayload.builder()
                    .type("PluginLoadError")
                    .message(throwable.getMessage())
                    .retryable(isRetryable(throwable))
                    .originNode("PluginLoader")
                    .timestamp(Instant.now())
                    .suggestedAction(determineSuggestedAction(throwable))
                    .details(Map.of("stackTrace", getStackTrace(throwable)))
                    .build();
                
                return ExecutionResult.error(error);
            })
            .onItem().transformToUni(result -> 
                // Log audit event
                auditService.logPluginLoad(pluginId, version, result.isSuccess())
                    .replaceWith(result)
            );
    }

    /**
     * Load plugin from downloaded artifact
     */
    private Uni<ExecutionResult<LoadedPlugin>> loadPluginFromArtifact(
            PluginDescriptor descriptor,
            byte[] artifactData,
            PluginKey key) {
        
        try {
            // Select isolation strategy based on sandbox level
            IsolationStrategy strategy = isolationManager.selectStrategy(
                descriptor.getSandboxLevel()
            );

            // Load plugin in isolated environment
            return strategy.loadPlugin(descriptor, artifactData)
                .onItem().transform(instance -> {
                    LoadedPlugin loaded = LoadedPlugin.builder()
                        .pluginId(descriptor.getId())
                        .version(descriptor.getVersion())
                        .descriptor(descriptor)
                        .instance(instance)
                        .strategy(strategy)
                        .loadedAt(Instant.now())
                        .build();

                    // Cache loaded plugin
                    loadedPlugins.put(key, loaded);

                    LOG.infof("Successfully loaded plugin: %s version %s", 
                        descriptor.getId(), descriptor.getVersion());

                    return ExecutionResult.success(loaded);
                })
                .onFailure().recoverWithItem(throwable -> {
                    ErrorPayload error = ErrorPayload.builder()
                        .type("PluginLoadError")
                        .message("Failed to instantiate plugin: " + throwable.getMessage())
                        .retryable(false)
                        .originNode("PluginLoader")
                        .timestamp(Instant.now())
                        .suggestedAction("human_review")
                        .build();
                    
                    return ExecutionResult.error(error);
                });

        } catch (Exception e) {
            ErrorPayload error = ErrorPayload.builder()
                .type("PluginLoadError")
                .message("Unexpected error: " + e.getMessage())
                .retryable(false)
                .originNode("PluginLoader")
                .timestamp(Instant.now())
                .suggestedAction("abort")
                .build();
            
            return Uni.createFrom().item(ExecutionResult.error(error));
        }
    }

    /**
     * Execute plugin node with error handling
     */
    public Uni<ExecutionResult<Object>> executeNode(
            String pluginId,
            String version,
            NodeContext context,
            int attempt,
            int maxAttempts) {
        
        PluginKey key = new PluginKey(pluginId, version);
        LoadedPlugin loaded = loadedPlugins.get(key);

        if (loaded == null) {
            ErrorPayload error = ErrorPayload.builder()
                .type("PluginLoadError")
                .message("Plugin not loaded: " + pluginId)
                .retryable(false)
                .originNode("PluginLoader")
                .timestamp(Instant.now())
                .suggestedAction("retry")
                .build();
            
            return Uni.createFrom().item(ExecutionResult.error(error));
        }

        // Execute with timeout and circuit breaker
        return loaded.getInstance().execute(context)
            .onItem().transform(result -> {
                // Success path
                return ExecutionResult.success(result.getOutputs());
            })
            .onFailure().recoverWithItem(throwable -> {
                // Error path - create ErrorPayload
                ErrorPayload error = ErrorPayload.builder()
                    .type(classifyError(throwable))
                    .message(throwable.getMessage())
                    .retryable(isRetryable(throwable))
                    .originNode(pluginId)
                    .attempt(attempt)
                    .maxAttempts(maxAttempts)
                    .timestamp(Instant.now())
                    .suggestedAction(determineSuggestedAction(throwable))
                    .details(Map.of(
                        "pluginId", pluginId,
                        "version", version,
                        "stackTrace", getStackTrace(throwable)
                    ))
                    .build();

                // Route to error handler
                return errorHandler.handleError(error, context)
                    .map(handled -> {
                        if (handled.shouldRetry() && attempt < maxAttempts) {
                            // Retry decision from error handler
                            return ExecutionResult.retry(error);
                        } else if (handled.shouldEscalate()) {
                            // Escalate to HITL
                            return ExecutionResult.escalate(error);
                        } else {
                            // Return error output
                            return ExecutionResult.error(error);
                        }
                    })
                    .await().indefinitely();
            });
    }

    /**
     * Unload plugin
     */
    public Uni<Boolean> unloadPlugin(String pluginId, String version) {
        PluginKey key = new PluginKey(pluginId, version);
        LoadedPlugin loaded = loadedPlugins.remove(key);

        if (loaded == null) {
            return Uni.createFrom().item(false);
        }

        return loaded.getStrategy().unloadPlugin(loaded.getInstance())
            .onItem().transformToUni(success -> 
                auditService.logPluginUnload(pluginId, version)
                    .replaceWith(success)
            )
            .onFailure().recoverWithItem(throwable -> {
                LOG.error("Failed to unload plugin", throwable);
                return false;
            });
    }

    /**
     * Classify error type
     */
    private String classifyError(Throwable throwable) {
        if (throwable instanceof TimeoutException) {
            return "Timeout";
        } else if (throwable instanceof SecurityException) {
            return "SecurityError";
        } else if (throwable instanceof ValidationException) {
            return "ValidationError";
        } else if (throwable instanceof NetworkException) {
            return "NetworkError";
        } else {
            return "UnknownError";
        }
    }

    /**
     * Determine if error is retryable
     */
    private boolean isRetryable(Throwable throwable) {
        return throwable instanceof TimeoutException 
            || throwable instanceof NetworkException
            || throwable instanceof TransientException;
    }

    /**
     * Determine suggested action based on error
     */
    private String determineSuggestedAction(Throwable throwable) {
        if (isRetryable(throwable)) {
            return "retry";
        } else if (throwable instanceof SecurityException) {
            return "abort";
        } else if (throwable instanceof ValidationException) {
            return "auto_fix";
        } else {
            return "human_review";
        }
    }

    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
