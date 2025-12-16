package tech.kayys.wayang.plugin.node;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.plugin.ExecutionResult;

/**
 * 
 * Base class for integration/connector/transformer nodes.
 * 
 * <p>
 * Integration nodes perform deterministic IO, transformation,
 * 
 * mapping, and communication with external systems.
 * 
 * They are intentionally lightweight:
 * 
 * <ul>
 * <li>No heavy guardrails</li>
 * <li>No LLM epistemic checks</li>
 * <li>No reasoning-specific policies</li>
 * <li>Fast execution for high-throughput ETL/API pipelines</li>
 * </ul>
 * <p>
 * Typical subclasses:
 * <ul>
 * <li>RestConnectorNode</li>
 * <li>KafkaProducerNode</li>
 * <li>JsonTransformerNode</li>
 * <li>MappingNode</li>
 * </ul>
 */
public abstract class IntegrationNode extends AbstractNode {
    @Override
    protected final Uni<ExecutionResult> doExecute(NodeContext context) {
        return executeIntegration(context);
    }

    /**
     * Actual integration logic to be implemented by subclasses.
     *
     * @param context execution context
     * @return result of integration operation
     */
    protected abstract Uni<ExecutionResult> executeIntegration(NodeContext context);
}