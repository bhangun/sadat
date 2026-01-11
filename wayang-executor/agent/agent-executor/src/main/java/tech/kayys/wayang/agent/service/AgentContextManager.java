package tech.kayys.wayang.agent.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.AgentConfiguration;
import tech.kayys.wayang.agent.model.AgentContext;

/**
 * Manages agent execution contexts
 */
@ApplicationScoped
public class AgentContextManager {

    private static final Logger LOG = LoggerFactory.getLogger(AgentContextManager.class);

    // Active contexts for running agents
    private final Map<String, AgentContext> activeContexts = new ConcurrentHashMap<>();

    /**
     * Create and register a new context
     */
    public AgentContext createContext(
            String sessionId,
            String runId,
            String nodeId,
            String tenantId,
            AgentConfiguration config,
            Map<String, Object> taskContext) {

        AgentContext context = AgentContext.builder()
                .sessionId(sessionId)
                .runId(runId)
                .nodeId(nodeId)
                .tenantId(tenantId)
                .configuration(config)
                .taskContext(taskContext)
                .build();

        String key = makeKey(sessionId, runId);
        activeContexts.put(key, context);

        LOG.debug("Created context: {}", key);
        return context;
    }

    /**
     * Get active context
     */
    public Optional<AgentContext> getContext(String sessionId, String runId) {
        String key = makeKey(sessionId, runId);
        return Optional.ofNullable(activeContexts.get(key));
    }

    /**
     * Remove context (cleanup after execution)
     */
    public void removeContext(String sessionId, String runId) {
        String key = makeKey(sessionId, runId);
        activeContexts.remove(key);
        LOG.debug("Removed context: {}", key);
    }

    /**
     * Get all active contexts for monitoring
     */
    public Map<String, AgentContext> getActiveContexts() {
        return new HashMap<>(activeContexts);
    }

    /**
     * Get active context count
     */
    public int getActiveContextCount() {
        return activeContexts.size();
    }

    private String makeKey(String sessionId, String runId) {
        return sessionId + ":" + runId;
    }
}
