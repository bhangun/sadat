package tech.kayys.wayang.plugin.node;

import io.smallrye.mutiny.Uni;
import java.util.Map;
import tech.kayys.wayang.plugin.ExecutionResult;
import tech.kayys.wayang.plugin.error.ErrorPayload;
import tech.kayys.wayang.plugin.guardrails.GuardrailResult;
import tech.kayys.wayang.plugin.guardrails.Guardrails;
import tech.kayys.wayang.plugin.node.obervability.Span;
import tech.kayys.wayang.plugin.node.obervability.Tracer;
import tech.kayys.wayang.plugin.validation.ValidationResult;

/**
 * Abstract base class providing common functionality for all nodes.
 * Handles error wrapping, observability, validation, and guardrails.
 *
 * <p>
 * Subclasses must implement {@link #doExecute(NodeContext)}.
 */
public abstract class AbstractNode implements Node {

    protected NodeDescriptor descriptor;
    protected NodeConfig config;
    protected MetricsCollector metrics;

    @Override
    public void onLoad(NodeDescriptor descriptor, NodeConfig config) {
        this.descriptor = descriptor;
        this.config = config;
        this.metrics = MetricsCollector.forNode(descriptor.getId());
        // Hook for subclass initialization
        doOnLoad(descriptor, config);
    }

    @Override
    public final Uni<ExecutionResult> execute(NodeContext context) {
        // Start tracing
        Span span = startTrace(context);
        long startTime = System.nanoTime();

        return Uni.createFrom().deferred(() -> {
            // Get services from context (injected by platform-core)
            Guardrails guardrails = context.getGuardrails();
            ProvenanceService provenance = context.getProvenance().getService();

            // Pre-execution guardrails (only if enabled)
            Uni<GuardrailResult> preCheck = Uni.createFrom().item(GuardrailResult::allow);
            if (config.guardrailsConfig().enabled()) {
                preCheck = guardrails.preCheck(context, descriptor);
            }

            return preCheck
                    .onItem().transformToUni(guardResult -> {
                        if (!guardResult.isAllowed()) {
                            return Uni.createFrom().item(
                                    ExecutionResult.blocked(guardResult.getReason()));
                        }

                        // Validate inputs against schema
                        return validateInputs(context)
                                .onItem().transformToUni(valid -> {
                                    if (!valid) {
                                        return Uni.createFrom().item(
                                                ExecutionResult.failed("Input validation failed"));
                                    }

                                    // Execute actual node logic
                                    return doExecute(context);
                                });
                    })
                    .onItem().transformToUni(result -> {
                        // Post-execution guardrails (only if enabled and result is success)
                        if (!config.guardrailsConfig().enabled() || !result.isSuccess()) {
                            return Uni.createFrom().item(result);
                        }
                        return guardrails.postCheck(result, descriptor)
                                .map(guardResult -> guardResult.isAllowed() ? result
                                        : ExecutionResult.blocked(guardResult.getReason()));
                    })
                    .onItem().invoke(result -> {
                        // Record metrics and provenance
                        long duration = System.nanoTime() - startTime;
                        metrics.recordExecution(duration, result.getStatus());
                        provenance.log(context.getNodeId(), context, result);
                    })
                    .onFailure().recoverWithItem(throwable -> {
                        // Wrap exceptions into structured error result
                        metrics.recordFailure(throwable);
                        return ExecutionResult.error(
                                ErrorPayload.from(throwable, descriptor.getId(), context));
                    })
                    .eventually(() -> endTrace(span));
        });
    }

    /**
     * Subclasses implement actual execution logic here.
     */
    protected abstract Uni<ExecutionResult> doExecute(NodeContext context);

    /**
     * Optional: Custom initialization logic.
     */
    protected void doOnLoad(NodeDescriptor descriptor, NodeConfig config) {
        // Default: no-op
    }

    /**
     * Validate inputs against declared schema.
     */
    private Uni<Boolean> validateInputs(NodeContext context) {
        return Uni.createFrom().item(() -> {
            for (var inputPort : descriptor.getInputs()) {
                var value = context.getInput(inputPort.getName());
                if (inputPort.isRequired() && value == null) {
                    return false;
                }
                if (value != null) {
                    Map<String, Object> schema = inputPort.getSchema();
                    if (schema != null && !SchemaUtils.validate(value, schema)) {
                        return false;
                    }
                }
            }
            return true;
        });
    }

    private Span startTrace(NodeContext context) {
        return Tracer.startSpan("node.execute")
                .withTag("node.id", descriptor.getId())
                .withTag("node.type", descriptor.getType())
                .withTag("run.id", context.getRunId())
                .withTag("tenant.id", context.getTenantId());
    }

    private void endTrace(Span span) {
        if (span != null) {
            span.finish();
        }
    }

    @Override
    public void onUnload() {
        if (metrics != null) {
            metrics.close();
        }
    }
}