package tech.kayys.wayang.workflow.model;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.schema.ErrorPayload;
import tech.kayys.wayang.schema.NodeDefinition;

/**
 * Healing strategy interface for deterministic repairs.
 */
interface HealingStrategy {
    boolean canHandle(String errorMessage);

    Uni<HealedContext> heal(
            NodeDefinition nodeDef,
            ExecutionContext context,
            ErrorPayload error);
}
