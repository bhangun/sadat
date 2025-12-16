package tech.kayys.wayang.plugin.node;

/**
 * Defines computational and operational limits for a node.
 * Used for cost control, throttling, and scheduling.
 */
public interface ResourceBudget {

    /**
     * Token budget (for LLM calls, embeddings, etc).
     */
    int getMaxTokens();

    /**
     * Maximum runtime in milliseconds.
     */
    int getMaxExecutionTimeMs();

    /**
     * Maximum allowed retries before failing permanently.
     */
    int getMaxRetries();

    /**
     * @return true if the budget is exhausted.
     */
    boolean isExhausted();
}
