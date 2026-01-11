package tech.kayys.wayang.agent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Structured logging for agent operations
 */
@ApplicationScoped
public class AgentStructuredLogger {

    private static final Logger LOG = LoggerFactory.getLogger(AgentStructuredLogger.class);

    /**
     * Log agent execution start
     */
    public void logExecutionStart(
            String runId,
            String nodeId,
            String tenantId,
            String sessionId) {

        LOG.info("Agent execution started: runId={}, nodeId={}, tenantId={}, sessionId={}",
                runId, nodeId, tenantId, sessionId);
    }

    /**
     * Log agent execution completion
     */
    public void logExecutionComplete(
            String runId,
            String nodeId,
            boolean success,
            int iterations,
            long durationMs,
            int totalTokens) {

        if (success) {
            LOG.info("Agent execution completed: runId={}, nodeId={}, " +
                    "iterations={}, durationMs={}, totalTokens={}",
                    runId, nodeId, iterations, durationMs, totalTokens);
        } else {
            LOG.error("Agent execution failed: runId={}, nodeId={}, " +
                    "iterations={}, durationMs={}",
                    runId, nodeId, iterations, durationMs);
        }
    }

    /**
     * Log LLM call
     */
    public void logLLMCall(
            String provider,
            String model,
            int messageCount,
            int promptTokens,
            int completionTokens) {

        LOG.debug("LLM call: provider={}, model={}, messages={}, " +
                "promptTokens={}, completionTokens={}",
                provider, model, messageCount, promptTokens, completionTokens);
    }

    /**
     * Log tool execution
     */
    public void logToolExecution(
            String toolName,
            boolean success,
            long durationMs,
            String error) {

        if (success) {
            LOG.debug("Tool executed: tool={}, durationMs={}",
                    toolName, durationMs);
        } else {
            LOG.warn("Tool execution failed: tool={}, durationMs={}, error={}",
                    toolName, durationMs, error);
        }
    }

    /**
     * Log memory operation
     */
    public void logMemoryOperation(
            String operation,
            String sessionId,
            int messageCount) {

        LOG.trace("Memory operation: op={}, sessionId={}, messageCount={}",
                operation, sessionId, messageCount);
    }
}
