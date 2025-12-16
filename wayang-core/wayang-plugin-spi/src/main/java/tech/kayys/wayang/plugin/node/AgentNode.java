package tech.kayys.wayang.plugin.node;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.plugin.ExecutionResult;

/**
 * 
 * Base class for all AI/LLM-driven nodes.
 * 
 * <p>
 * Agent nodes perform reasoning, planning, tool invocation,
 * 
 * and may require strong safety/guardrail enforcement.
 * 
 * This class extends {@link AbstractNode} and adds:
 * 
 * <ul>
 * <li>Epistemic safety checks</li>
 * <li>LLM rate limiting</li>
 * <li>Hallucination guardrails</li>
 * <li>Tool-call safety</li>
 * <li>Retry and fallback policies</li>
 * </ul>
 * <p>
 * Typical subclasses:
 * <ul>
 * <li>LLMCompletionNode</li>
 * <li>AgentPlannerNode</li>
 * <li>RAGNode</li>
 * <li>ToolExecutorNode</li>
 * </ul>
 * 
 */
public abstract class AgentNode extends AbstractNode {
    /**
     * Performs optional pre-execution AI-specific validations.
     * Override if the agent requires additional safety steps.
     *
     * @param context node execution context
     * @return Uni resolved when safety checks are complete
     */
    protected Uni<Void> preAgentSafety(NodeContext context) {
        return Uni.createFrom().voidItem();
    }

    /**
     * Performs optional post-execution sanity checks on the LLM output
     * (e.g., hallucination filters, JSON schema repair).
     *
     * @param result execution result
     * @return validated/fixed result
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