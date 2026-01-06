package tech.kayys.wayang.api.node;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.api.execution.ExecutionResult;
import tech.kayys.wayang.api.guardrails.GuardrailResult;
import tech.kayys.wayang.api.node.obervability.Span;
import tech.kayys.wayang.api.node.obervability.Tracer;

import java.util.Map;

import io.smallrye.mutiny.Uni;

/**
 * Base class for all executable nodes in the workflow engine.
 * <p>
 * This class provides standardized behavior for:
 * <ul>
 * <li>Guardrail enforcement (pre/post checks)</li>
 * <li>Input schema validation</li>
 * <li>Metrics collection</li>
 * <li>Tracing and observability</li>
 * <li>Error wrapping into structured {@link ExecutionResult}</li>
 * <li>Automatic provenance logging</li>
 * </ul>
 *
 * <p>
 * Subclasses must implement the {@link #doExecute(NodeContext)} method.
 * All other lifecycle behavior is automatically handled by this class.
 * </p>
 *
 * <p>
 * This abstraction is intentionally neutral: it supports AI agent nodes,
 * pure data transformation nodes, connector nodes, enrichment nodes,
 * branching nodes, and any future node type without modification.
 * </p>
 */
public abstract class AbstractNode implements Node {

    /** Static descriptor of this node, loaded during plugin registration. */
    protected NodeDescriptor descriptor;

    /** Node configuration instance passed from platform/core. */
    protected NodeConfig config;

    /** Metrics collector for execution timing, failures, etc. */
    protected MetricsCollector metrics;

    /**
     * Called once when the node implementation is loaded.
     *
     * @param descriptor structural description (ports, schemas, categories)
     * @param config     runtime configuration (guardrails, model selection, etc.)
     */
    @Override
    public Uni<Void> onLoad(NodeDescriptor descriptor, NodeConfig config) {
        this.descriptor = descriptor;
        this.config = config;
        this.metrics = MetricsCollector.forNode(descriptor.getId());
        doOnLoad(descriptor, config);
        return Uni.createFrom().voidItem();
    }

    /**
     * Template method implementing the full execution pipeline.
     * <p>
     * The method performs:
     * <ol>
     * <li>Tracing start</li>
     * <li>Guardrail pre-check</li>
     * <li>Input schema validation</li>
     * <li>Delegation to {@link #doExecute(NodeContext)}</li>
     * <li>Guardrail post-check</li>
     * <li>Metrics + provenance logging</li>
     * <li>Error wrapping</li>
     * <li>Tracing end</li>
     * </ol>
     *
     * <p>
     * This method must be idempotent and non-blocking.
     * All blocking work should use Mutiny reactive helpers.
     * </p>
     *
     * @param context runtime execution context
     * @return a Uni emitting {@link ExecutionResult}
     */
    @Override
    public final Uni<ExecutionResult> execute(NodeContext context) {

        Span span = startTrace(context);
        long startTime = System.nanoTime();

        return Uni.createFrom().deferred(() -> {
            Guardrails guardrails = context.getGuardrails();
            ProvenanceService provenance = context.getProvenance().getService();

            // 1. Pre-check
            Uni<GuardrailResult> preCheck = config.guardrailsConfig().enabled()
                    ? guardrails.preCheck(context, descriptor)
                    : Uni.createFrom().item(GuardrailResult.allow());

            return preCheck
                    .onItem().transformToUni(guardResult -> {
                        if (!guardResult.isAllowed()) {
                            return Uni.createFrom().item(
                                    ExecutionResult.blocked(guardResult.getReason()));
                        }

                        // 2. Validate inputs
                        return validateInputs(context)
                                .onItem().transformToUni(valid -> {
                                    if (!valid) {
                                        return Uni.createFrom().item(
                                                ExecutionResult.failed("Input validation failed"));
                                    }

                                    // 3. Execute the node logic
                                    return doExecute(context);
                                });
                    })

                    // 4. Post-check
                    .onItem().transformToUni(result -> {
                        if (!config.guardrailsConfig().enabled() || !result.isSuccess()) {
                            return Uni.createFrom().item(result);
                        }

                        return guardrails.postCheck(result, descriptor)
                                .map(g -> g.isAllowed()
                                        ? result
                                        : ExecutionResult.blocked(g.getReason()));
                    })

                    // 5. Metrics + provenance
                    .onItem().invoke(result -> {
                        long duration = System.nanoTime() - startTime;
                        metrics.recordExecution(duration, result.getStatus());
                        provenance.log(context.getNodeId(), context, result);
                    })

                    // 6. Error handling
                    .onFailure().recoverWithItem(th -> {
                        metrics.recordFailure(th);
                        return ExecutionResult.error(
                                ErrorPayload.from(th, descriptor.getId(), context));
                    })

                    // 7. Close span
                    .eventually(() -> endTrace(span));
        });
    }

    /**
     * Performs the actual execution of the node.
     * <p>
     * This is the only method subclasses need to implement.
     * The method:
     * <ul>
     * <li>must not perform blocking operations</li>
     * <li>must return a Uni</li>
     * <li>must be idempotent</li>
     * </ul>
     *
     * @param context runtime context containing inputs, variables, services
     * @return a Uni of {@link ExecutionResult}
     */
    protected abstract Uni<ExecutionResult> doExecute(NodeContext context);

    /**
     * Optional hook executed after node metadata/config is loaded.
     * Subclasses may override to perform initialization.
     *
     * @param descriptor node definition
     * @param config     node configuration from platform
     */
    protected void doOnLoad(NodeDescriptor descriptor, NodeConfig config) {
        // Default: no-op
    }

    /**
     * Validates node inputs against the declared schemas in {@link NodeDescriptor}.
     * Ensures type-safety and future-forward compatibility for dynamic nodes.
     *
     * @param context runtime context
     * @return Uni emitting true if valid, false otherwise
     */
    private Uni<Boolean> validateInputs(NodeContext context) {
        return Uni.createFrom().item(() -> {
            for (var input : descriptor.getInputs()) {
                Object value = context.getInput(input.getName());

                if (input.isRequired() && value == null) {
                    return false;
                }

                if (value != null && input.getSchema() != null) {
                    if (!SchemaUtils.validate(value, input.getSchema())) {
                        return false;
                    }
                }
            }
            return true;
        });
    }

    /**
     * Starts a tracing span for the node.
     *
     * @param context runtime execution context
     * @return a span object (no-op implementation allowed)
     */
    private Span startTrace(NodeContext context) {
        return Tracer.spanBuilder("node.execute")
                .withTag("node.id", descriptor.getId())
                .withTag("node.type", descriptor.getType())
                .withTag("run.id", context.getRunId())
                .withTag("tenant.id", context.getTenantId());
    }

    /**
     * Ends the tracing span.
     *
     * @param span tracing span
     */
    private void endTrace(Span span) {
        if (span != null) {
            span.finish();
        }
    }

    /**
     * Called when the node is unloaded (e.g., plugin hot reload).
     * Ensures internal metrics are properly closed.
     */
    @Override
    public Uni<Void> onUnload() {
        if (metrics != null) {
            metrics.close();
        }
        return Uni.createFrom().voidItem();
    }
}
