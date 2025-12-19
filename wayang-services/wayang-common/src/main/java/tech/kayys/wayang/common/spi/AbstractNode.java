package tech.kayys.wayang.common.spi;

import io.smallrye.mutiny.Uni;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.schema.execution.ErrorPayload;
import tech.kayys.wayang.schema.node.NodeDefinition;
import tech.kayys.wayang.workflow.api.dto.AuditPayload;
import tech.kayys.wayang.workflow.api.dto.AuditPayload.AuditLevel;
import tech.kayys.wayang.workflow.api.dto.AuditPayload.Events;

import java.util.Map;

/**
 * AbstractNode: Base implementation following Blueprint pattern.
 * 
 * Responsibilities:
 * - Pre-check guardrails
 * - Input validation
 * - Execute derived node logic
 * - Post-check guardrails
 * - Metrics & provenance recording
 * - Error handling with ErrorPayload
 * - Tracing
 * 
 * Derived classes:
 * - IntegrationNode: for deterministic I/O, transformations
 * - AgentNode: for LLM-driven reasoning with additional safety
 */
public abstract class AbstractNode implements Node {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected NodeDefinition descriptor;
    protected NodeConfig config;
    protected MetricsCollector metrics;

    @Override
    public Uni<Void> onLoad(NodeDefinition descriptor, NodeConfig config) {
        this.descriptor = descriptor;
        this.config = config;
        this.metrics = MetricsCollector.forNode(descriptor.getId());

        log.info("Loading node: {} (type: {})", descriptor.getId(), descriptor.getType());

        return doOnLoad(descriptor, config)
                .onFailure().invoke(th -> log.error("Failed to load node {}", descriptor.getId(), th));
    }

    @Override
    public final Uni<ExecutionResult> execute(NodeContext context) {
        Span span = startTrace(context);
        long startTime = System.nanoTime();

        return Uni.createFrom().deferred(() -> {
            Guardrails guardrails = context.getGuardrails();
            ProvenanceContext provenance = context.getProvenance();

            // 1. Pre-check guardrails
            Uni<GuardrailResult> preCheck = config.guardrailsConfig().enabled()
                    ? guardrails.preCheck(context, descriptor)
                    : Uni.createFrom().item(GuardrailResult.allow());

            return preCheck
                    .onItem().transformToUni(guardResult -> {
                        if (!guardResult.isAllowed()) {
                            log.warn("Guardrail pre-check blocked node {} in run {}: {}",
                                    context.getNodeId(), context.getRunId(), guardResult.getReason());
                            return Uni.createFrom().item(
                                    ExecutionResult.blocked(guardResult.getReason()));
                        }

                        // 2. Validate inputs
                        return validateInputs(context)
                                .onItem().transformToUni(valid -> {
                                    if (!valid) {
                                        ErrorPayload err = ErrorPayload.builder()
                                                .type(ErrorPayload.ErrorType.VALIDATION_ERROR)
                                                .message("Input validation failed")
                                                .originNode(context.getNodeId())
                                                .originRunId(context.getRunId().toString())
                                                .retryable(false)
                                                .suggestedAction(ErrorPayload.SuggestedAction.AUTO_FIX)
                                                .build();
                                        return Uni.createFrom().item(ExecutionResult.error(err));
                                    }

                                    // 3. Execute the node logic (implemented by subclass)
                                    return doExecute(context);
                                });
                    })

                    // 4. Post-check guardrails
                    .onItem().transformToUni(result -> {
                        if (!config.guardrailsConfig().enabled() || !result.isSuccess()) {
                            return Uni.createFrom().item(result);
                        }

                        return guardrails.postCheck(result, descriptor)
                                .map(g -> {
                                    if (!g.isAllowed()) {
                                        log.warn("Guardrail post-check blocked output from node {}: {}",
                                                context.getNodeId(), g.getReason());
                                        return ExecutionResult.blocked(g.getReason());
                                    }
                                    return result;
                                });
                    })

                    // 5. Record metrics & provenance
                    .onItem().invoke(result -> {
                        long durationNs = System.nanoTime() - startTime;
                        metrics.recordExecution(durationNs, result.status());

                        // Log audit event
                        provenance.log(
                                AuditPayload.builder()
                                        .runId(context.getRunId())
                                        .nodeId(context.getNodeId())
                                        .systemActor()
                                        .event(result.isSuccess() ? Events.NODE_SUCCESS
                                                : Events.NODE_ERROR)
                                        .level(result.isError() ? AuditLevel.ERROR : AuditLevel.INFO)
                                        .metadata(Map.of(
                                                "durationMs", durationNs / 1_000_000,
                                                "outputChannel", result.outputChannel() != null ? result.outputChannel() : "default"))
                                        .build());
                    })

                    // 6. Error handling
                    .onFailure().recoverWithItem(th -> {
                        log.error("Node {} execution failed in run {}",
                                context.getNodeId(), context.getRunId(), th);

                        metrics.recordFailure(th);

                        ErrorPayload err = ErrorPayload.fromThrowable(
                                th,
                                context.getNodeId(),
                                context.getRunId());

                        return ExecutionResult.error(err);
                    })

                    // 7. Close trace span
                    .eventually(() -> {
                        endTrace(span);
                        return Uni.createFrom().voidItem();
                    });
        });
    }

    /**
     * Subclasses implement node-specific logic here.
     * Must not throw exceptions - return error results instead.
     */
    protected abstract Uni<ExecutionResult> doExecute(NodeContext context);

    /**
     * Optional initialization hook
     */
    protected Uni<Void> doOnLoad(NodeDefinition descriptor, NodeConfig config) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Validate inputs against descriptor schema
     */
    private Uni<Boolean> validateInputs(NodeContext context) {
        return Uni.createFrom().item(() -> {
            for (var inputDesc : descriptor.getInputs()) {
                Object value = context.getInput(inputDesc.getName());

                if (inputDesc.getRequired() && value == null) {
                    log.error("Required input '{}' is missing", inputDesc.getName());
                    return false;
                }

                if (value != null && inputDesc.getSchema() != null) {
                    if (!SchemaValidator.validate(value, inputDesc.getSchema())) {
                        log.error("Input '{}' failed schema validation", inputDesc.getName());
                        return false;
                    }
                }
            }
            return true;
        });
    }

    private Span startTrace(NodeContext context) {
        return Tracer.spanBuilder("node.execute")
                .withTag("node.id", descriptor.getId())
                .withTag("node.type", descriptor.getType())
                .withTag("run.id", context.getRunId())
                .withTag("tenant.id", context.getTenantId())
                .start();
    }

    private void endTrace(Span span) {
        if (span != null) {
            span.finish();
        }
    }

    @Override
    public boolean isStateless() {
        return true;
    }

    @Override
    public Uni<Void> onUnload() {
        log.info("Unloading node: {}", descriptor.getId());
        if (metrics != null) {
            metrics.close();
        }
        return Uni.createFrom().voidItem();
    }
}