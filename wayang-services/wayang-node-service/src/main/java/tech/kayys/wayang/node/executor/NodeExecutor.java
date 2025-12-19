package tech.kayys.wayang.node.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.client.GuardrailsClient.GuardrailResult;
import tech.kayys.wayang.schema.ErrorPayload;

import org.jboss.logging.Logger;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * NodeExecutor - Executes individual nodes with full lifecycle management.
 * 
 * Responsibilities:
 * - Load and manage node instances (plugins, agents, connectors)
 * - Execute nodes with proper context and inputs
 * - Apply pre/post guardrails
 * - Handle validation and schema checking
 * - Emit telemetry and audit events
 * - Manage node lifecycle (load, execute, unload)
 * 
 * Design Principles:
 * - Type-safe node execution (IntegrationNode vs AgentNode)
 * - Isolation between nodes (no shared state)
 * - Deterministic execution flow
 * - Resource management (timeout, memory limits)
 * - Hot-reload support for plugin updates
 */
@ApplicationScoped
public class NodeExecutor {

    private static final Logger LOG = Logger.getLogger(NodeExecutor.class);

    @Inject
    NodeRegistry nodeRegistry;

    @Inject
    GuardrailsEngine guardrails;

    @Inject
    ProvenanceService provenance;

    @Inject
    TelemetryService telemetry;

    @Inject
    SchemaValidator schemaValidator;

    @Inject
    ResourceProfileManager resourceManager;

    // Cache of loaded node instances
    private final Map<String, Node> nodeCache = new ConcurrentHashMap<>();

    /**
     * Execute a node with full lifecycle and error handling.
     * 
     * @param nodeDef Node definition from workflow
     * @param context Node execution context with inputs
     * @return NodeExecutionResult with outputs or error
     */
    public Uni<NodeExecutionResult> execute(
            NodeDefinition nodeDef,
            NodeContext context) {

        String nodeId = nodeDef.getId();
        LOG.debugf("Executing node: %s (type: %s)", nodeId, nodeDef.getType());

        long startTime = System.nanoTime();

        return Uni.createFrom().deferred(() -> {

            // 1. Load node instance
            return loadNode(nodeDef)
                    .onItem().transformToUni(node -> {

                        // 2. Apply pre-execution guardrails
                        return applyPreGuardrails(node, nodeDef, context)
                                .onItem().transformToUni(guardResult -> {

                                    if (!guardResult.isAllowed()) {
                                        // Blocked by guardrails
                                        return Uni.createFrom().item(
                                                NodeExecutionResult.blocked(
                                                        nodeId,
                                                        guardResult.getReason()));
                                    }

                                    // 3. Validate inputs against schema
                                    return validateInputs(nodeDef, context)
                                            .onItem().transformToUni(validationResult -> {

                                                if (!validationResult.isValid()) {
                                                    // Input validation failed
                                                    ErrorPayload error = ErrorPayload.builder()
                                                            .type(ErrorType.VALIDATION_ERROR)
                                                            .message("Input validation failed: " +
                                                                    validationResult.getMessage())
                                                            .originNode(nodeId)
                                                            .originRunId(context.getRunId())
                                                            .retryable(false)
                                                            .timestamp(Instant.now())
                                                            .suggestedAction(ErrorAction.HUMAN_REVIEW)
                                                            .details(validationResult.getErrors())
                                                            .build();

                                                    return Uni.createFrom().item(
                                                            NodeExecutionResult.error(nodeId, error));
                                                }

                                                // 4. Apply resource limits
                                                return applyResourceLimits(nodeDef, context)
                                                        .onItem().transformToUni(v -> {

                                                            // 5. Execute the node
                                                            return executeNodeWithTimeout(
                                                                    node,
                                                                    context,
                                                                    nodeDef.getResourceProfile());
                                                        });
                                            });
                                });
                    })
                    .onItem().transformToUni(result -> {
                        // 6. Apply post-execution guardrails
                        if (result.isSuccess()) {
                            return applyPostGuardrails(result, nodeDef, context)
                                    .map(guardResult -> {
                                        if (!guardResult.isAllowed()) {
                                            return NodeExecutionResult.blocked(
                                                    nodeId,
                                                    guardResult.getReason());
                                        }
                                        return result;
                                    });
                        }
                        return Uni.createFrom().item(result);
                    })
                    .onItem().transformToUni(result -> {
                        // 7. Validate outputs against schema
                        if (result.isSuccess()) {
                            return validateOutputs(nodeDef, result, context)
                                    .map(validationResult -> {
                                        if (!validationResult.isValid()) {
                                            ErrorPayload error = ErrorPayload.builder()
                                                    .type(ErrorType.VALIDATION_ERROR)
                                                    .message("Output validation failed: " +
                                                            validationResult.getMessage())
                                                    .originNode(nodeId)
                                                    .originRunId(context.getRunId())
                                                    .retryable(true)
                                                    .timestamp(Instant.now())
                                                    .suggestedAction(ErrorAction.AUTO_FIX)
                                                    .details(validationResult.getErrors())
                                                    .build();

                                            return NodeExecutionResult.error(nodeId, error);
                                        }
                                        return result;
                                    });
                        }
                        return Uni.createFrom().item(result);
                    })
                    .onItem().invoke(result -> {
                        // 8. Record metrics
                        long duration = System.nanoTime() - startTime;
                        telemetry.recordNodeExecution(
                                nodeId,
                                nodeDef.getType(),
                                duration,
                                result.getStatus());
                    })
                    .onItem().call(result -> {
                        // 9. Log to provenance
                        return provenance.logNodeExecution(context, nodeDef, result);
                    })
                    .onFailure().recoverWithItem(th -> {
                        // 10. Handle unexpected errors
                        LOG.errorf(th, "Unexpected error executing node: %s", nodeId);

                        ErrorPayload error = ErrorPayload.builder()
                                .type(ErrorType.UNKNOWN_ERROR)
                                .message(th.getMessage())
                                .originNode(nodeId)
                                .originRunId(context.getRunId())
                                .retryable(true)
                                .timestamp(Instant.now())
                                .suggestedAction(ErrorAction.RETRY)
                                .stackTrace(getStackTrace(th))
                                .build();

                        return NodeExecutionResult.error(nodeId, error);
                    });
        });
    }

    /**
     * Load node instance from registry or cache.
     */
    private Uni<Node> loadNode(NodeDefinition nodeDef) {
        String nodeType = nodeDef.getType();

        // Check cache first
        Node cached = nodeCache.get(nodeType);
        if (cached != null) {
            return Uni.createFrom().item(cached);
        }

        // Load from registry
        return nodeRegistry.load(nodeType)
                .onItem().transformToUni(node -> {
                    // Initialize node
                    NodeDescriptor descriptor = nodeRegistry.getDescriptor(nodeType);
                    NodeConfig config = NodeConfig.from(nodeDef);

                    return node.onLoad(descriptor, config)
                            .replaceWith(node)
                            .onItem().invoke(loadedNode -> {
                                // Cache for reuse
                                nodeCache.put(nodeType, loadedNode);
                            });
                });
    }

    /**
     * Execute node with timeout protection.
     */
    private Uni<NodeExecutionResult> executeNodeWithTimeout(
            Node node,
            NodeContext context,
            ResourceProfile profile) {

        long timeoutMs = profile != null ? profile.getTimeoutMs() : 30000;

        return node.execute(context)
                .ifNoItem().after(java.time.Duration.ofMillis(timeoutMs))
                .recoverWithItem(() -> {
                    ErrorPayload error = ErrorPayload.builder()
                            .type(ErrorType.TIMEOUT)
                            .message("Node execution timeout after " + timeoutMs + "ms")
                            .originNode(context.getNodeId())
                            .originRunId(context.getRunId())
                            .retryable(true)
                            .timestamp(Instant.now())
                            .suggestedAction(ErrorAction.RETRY)
                            .build();

                    return NodeExecutionResult.error(context.getNodeId(), error);
                });
    }

    /**
     * Apply pre-execution guardrails.
     */
    private Uni<GuardrailResult> applyPreGuardrails(
            Node node,
            NodeDefinition nodeDef,
            NodeContext context) {

        if (nodeDef.getPolicy() == null ||
                !nodeDef.getPolicy().isGuardrailsEnabled()) {
            return Uni.createFrom().item(GuardrailResult.allow());
        }

        return guardrails.evaluatePreExecution(
                nodeDef,
                context,
                node instanceof AgentNode);
    }

    /**
     * Apply post-execution guardrails.
     */
    private Uni<GuardrailResult> applyPostGuardrails(
            NodeExecutionResult result,
            NodeDefinition nodeDef,
            NodeContext context) {

        if (nodeDef.getPolicy() == null ||
                !nodeDef.getPolicy().isGuardrailsEnabled()) {
            return Uni.createFrom().item(GuardrailResult.allow());
        }

        return guardrails.evaluatePostExecution(
                nodeDef,
                context,
                result);
    }

    /**
     * Validate node inputs against schema.
     */
    private Uni<ValidationResult> validateInputs(
            NodeDefinition nodeDef,
            NodeContext context) {

        return Uni.createFrom().item(() -> {
            List<String> errors = new ArrayList<>();

            for (PortDescriptor input : nodeDef.getInputs()) {
                Object value = context.getInput(input.getName());

                // Check required
                if (input.getData().isRequired() && value == null) {
                    errors.add("Required input '" + input.getName() + "' is missing");
                    continue;
                }

                // Validate against schema
                if (value != null && input.getData().getSchema() != null) {
                    ValidationResult schemaResult = schemaValidator.validate(
                            value,
                            input.getData().getSchema());

                    if (!schemaResult.isValid()) {
                        errors.add("Input '" + input.getName() + "' validation failed: " +
                                schemaResult.getMessage());
                    }
                }
            }

            if (errors.isEmpty()) {
                return ValidationResult.success();
            }

            return ValidationResult.failure(
                    "Input validation failed",
                    errors);
        });
    }

    /**
     * Validate node outputs against schema.
     */
    private Uni<ValidationResult> validateOutputs(
            NodeDefinition nodeDef,
            NodeExecutionResult result,
            NodeContext context) {

        return Uni.createFrom().item(() -> {
            List<String> errors = new ArrayList<>();

            if (nodeDef.getOutputs() == null ||
                    nodeDef.getOutputs().getChannels() == null) {
                return ValidationResult.success();
            }

            for (OutputChannel channel : nodeDef.getOutputs().getChannels()) {
                Object value = result.getOutputChannels().get(channel.getName());

                // Check required outputs
                if (channel.isRequired() && value == null) {
                    errors.add("Required output '" + channel.getName() + "' is missing");
                    continue;
                }

                // Validate against schema
                if (value != null && channel.getSchema() != null) {
                    ValidationResult schemaResult = schemaValidator.validate(
                            value,
                            channel.getSchema());

                    if (!schemaResult.isValid()) {
                        errors.add("Output '" + channel.getName() + "' validation failed: " +
                                schemaResult.getMessage());
                    }
                }
            }

            if (errors.isEmpty()) {
                return ValidationResult.success();
            }

            return ValidationResult.failure(
                    "Output validation failed",
                    errors);
        });
    }

    /**
     * Apply resource limits for node execution.
     */
    private Uni<Void> applyResourceLimits(
            NodeDefinition nodeDef,
            NodeContext context) {

        ResourceProfile profile = nodeDef.getResourceProfile();
        if (profile == null) {
            return Uni.createFrom().voidItem();
        }

        return resourceManager.allocate(context.getNodeId(), profile)
                .onItem().invoke(() -> {
                    LOG.debugf("Applied resource limits for node %s: cpu=%s, memory=%s",
                            context.getNodeId(),
                            profile.getCpu(),
                            profile.getMemory());
                });
    }

    /**
     * Unload a node and clear from cache.
     */
    public Uni<Void> unloadNode(String nodeType) {
        Node node = nodeCache.remove(nodeType);
        if (node != null) {
            return node.onUnload()
                    .onItem().invoke(() -> LOG.infof("Unloaded node: %s", nodeType));
        }
        return Uni.createFrom().voidItem();
    }

    /**
     * Clear all cached nodes (for hot-reload).
     */
    public Uni<Void> clearCache() {
        List<Uni<Void>> unloadOps = nodeCache.values().stream()
                .map(Node::onUnload)
                .toList();

        return Uni.combine().all().unis(unloadOps)
                .discardItems()
                .onItem().invoke(() -> {
                    nodeCache.clear();
                    LOG.info("Cleared node cache");
                });
    }

    /**
     * Get cache statistics.
     */
    public Map<String, Object> getCacheStats() {
        return Map.of(
                "size", nodeCache.size(),
                "types", nodeCache.keySet());
    }

    private String getStackTrace(Throwable th) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        th.printStackTrace(pw);
        return sw.toString();
    }
}
