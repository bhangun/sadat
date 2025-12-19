package tech.kayys.wayang.common.spi;

import io.smallrye.mutiny.Uni;
/**
 * AgentNode: For LLM-driven reasoning nodes
 * Adds additional safety layers for AI operations
 */
public abstract class AgentNode extends AbstractNode {
    
    /**
     * Optional pre-execution AI-specific safety checks
     * Examples: prompt injection detection, content filtering
     */
    protected Uni<Void> preAgentSafety(NodeContext context) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Optional post-execution validation
     * Examples: hallucination detection, JSON repair
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
     * Implement agent reasoning logic here.
     * May involve LLM calls, tool usage, memory access.
     */
    protected abstract Uni<ExecutionResult> executeAgent(NodeContext context);
}