
```java
public abstract class AbstractNode implements Node {

    protected NodeDescriptor descriptor;

    protected NodeConfig config;

    protected MetricsCollector metrics;


    @Override
    public Uni<Void> onLoad(NodeDescriptor descriptor, NodeConfig config) {
        this.descriptor = descriptor;
        this.config = config;
        this.metrics = MetricsCollector.forNode(descriptor.getId());
        doOnLoad(descriptor, config);
        return Uni.createFrom().voidItem();
    }


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


    protected abstract Uni<ExecutionResult> doExecute(NodeContext context);

    protected void doOnLoad(NodeDescriptor descriptor, NodeConfig config) {
        // Default: no-op
    }

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

    private Span startTrace(NodeContext context) {
        return Tracer.spanBuilder("node.execute")
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
    public Uni<Void> onUnload() {
        if (metrics != null) {
            metrics.close();
        }
        return Uni.createFrom().voidItem();
    }
}


/**
 * Base class for integration/connector/transformer nodes.
 * Integration nodes perform deterministic IO, transformation,
 * mapping, and communication with external systems.
 * They are intentionally lightweight:
 */
public abstract class IntegrationNode extends AbstractNode {
    @Override
    protected final Uni<ExecutionResult> doExecute(NodeContext context) {
        return executeIntegration(context);
    }

    protected abstract Uni<ExecutionResult> executeIntegration(NodeContext context);
}

/**
 * Base class for all AI/LLM-driven nodes.
 * Agent nodes perform reasoning, planning, tool invocation,
 * and may require strong safety/guardrail enforcement.
 */
public abstract class AgentNode extends AbstractNode {
    /**
     * Performs optional pre-execution AI-specific validations.
     * Override if the agent requires additional safety steps.
     */
    protected Uni<Void> preAgentSafety(NodeContext context) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Performs optional post-execution sanity checks on the LLM output
     * (e.g., hallucination filters, JSON schema repair).
*/
    protected ExecutionResult postAgentValidation(ExecutionResult result) {
        return result;
    }

    @Override
    protected final Uni<ExecutionResult> doExecute(NodeContext context) {
        return preAgentSafety(context)
                .onItem().transformToUni(v -> executeAgent(context))
                .map(this::postAgentValidation);
    }

    /**
     * Agent-specific execution logic. Subclasses must implement.
     */
    protected abstract Uni<ExecutionResult> executeAgent(NodeContext context);
}
```