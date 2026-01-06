package tech.kayys.wayang.agent.orchestrator.service;


import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * AGENT REGISTRY
 * ============================================================================
 * 
 * Centralized registry for agent discovery and management
 * 
 * Features:
 * - Agent registration and deregistration
 * - Health monitoring
 * - Capability-based discovery
 * - Load balancing
 * - Multi-tenant isolation
 */
@ApplicationScoped
public class AgentRegistry {
    
    private static final Logger LOG = LoggerFactory.getLogger(AgentRegistry.class);
    private static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(30);
    
    // In-memory registry (backed by database)
    private final ConcurrentMap<String, AgentRegistration> agents = new ConcurrentHashMap<>();
    
    @Inject
    AgentPersistence persistence;
    
    /**
     * Register new agent
     */
    public Uni<AgentRegistration> registerAgent(
            String agentId,
            String agentName,
            AgentType agentType,
            Set<AgentCapability> capabilities,
            AgentEndpoint endpoint,
            String tenantId) {
        
        LOG.info("Registering agent: {} (type: {})", agentId, agentType.typeName());
        
        AgentRegistration registration = new AgentRegistration(
            agentId,
            agentName,
            agentType,
            capabilities,
            AgentStatus.AVAILABLE,
            endpoint,
            Map.of(
                "tenantId", tenantId,
                "successRate", "1.0",
                "currentLoad", "0.0"
            ),
            Instant.now(),
            Instant.now()
        );
        
        agents.put(agentId, registration);
        
        return persistence.saveAgent(registration)
            .replaceWith(registration);
    }
    
    /**
     * Deregister agent
     */
    public Uni<Void> deregisterAgent(String agentId) {
        LOG.info("Deregistering agent: {}", agentId);
        
        agents.remove(agentId);
        
        return persistence.deleteAgent(agentId);
    }
    
    /**
     * Update agent heartbeat
     */
    public Uni<Void> updateHeartbeat(String agentId) {
        AgentRegistration current = agents.get(agentId);
        if (current == null) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("Agent not found: " + agentId));
        }
        
        AgentRegistration updated = new AgentRegistration(
            current.agentId(),
            current.agentName(),
            current.agentType(),
            current.capabilities(),
            current.status(),
            current.endpoint(),
            current.metadata(),
            current.registeredAt(),
            Instant.now()
        );
        
        agents.put(agentId, updated);
        
        return persistence.updateHeartbeat(agentId, Instant.now());
    }
    
    /**
     * Find available agents by type and capabilities
     */
    public Uni<List<AgentRegistration>> findAvailableAgents(
            String agentType,
            Set<AgentCapability> requiredCapabilities) {
        
        return Uni.createFrom().item(() -> 
            agents.values().stream()
                .filter(AgentRegistration::isAvailable)
                .filter(AgentRegistration::isHealthy)
                .filter(agent -> matchesType(agent, agentType))
                .filter(agent -> hasCapabilities(agent, requiredCapabilities))
                .collect(Collectors.toList())
        );
    }
    
    /**
     * Get agent by ID
     */
    public Uni<AgentRegistration> getAgent(String agentId) {
        AgentRegistration agent = agents.get(agentId);
        if (agent == null) {
            return Uni.createFrom().nullItem();
        }
        return Uni.createFrom().item(agent);
    }
    
    /**
     * Update agent status
     */
    public Uni<Void> updateStatus(String agentId, AgentStatus status) {
        AgentRegistration current = agents.get(agentId);
        if (current == null) {
            return Uni.createFrom().failure(
                new IllegalArgumentException("Agent not found"));
        }
        
        AgentRegistration updated = new AgentRegistration(
            current.agentId(),
            current.agentName(),
            current.agentType(),
            current.capabilities(),
            status,
            current.endpoint(),
            current.metadata(),
            current.registeredAt(),
            current.lastHeartbeat()
        );
        
        agents.put(agentId, updated);
        
        return persistence.updateStatus(agentId, status);
    }
    
    /**
     * List all agents (for monitoring)
     */
    public Uni<List<AgentRegistration>> listAllAgents(String tenantId) {
        return Uni.createFrom().item(() ->
            agents.values().stream()
                .filter(agent -> agent.metadata().get("tenantId").equals(tenantId))
                .collect(Collectors.toList())
        );
    }
    
    // ==================== HELPER METHODS ====================
    
    private boolean matchesType(AgentRegistration agent, String requestedType) {
        return agent.agentType().typeName().equalsIgnoreCase(requestedType) ||
               requestedType.equalsIgnoreCase("ANY");
    }
    
    private boolean hasCapabilities(
            AgentRegistration agent,
            Set<AgentCapability> required) {
        return agent.capabilities().containsAll(required);
    }
    
    /**
     * Background task to clean up stale agents
     */
    @jakarta.enterprise.event.Observes
    void onStartup(@jakarta.enterprise.event.Startup event) {
        // Start cleanup job
        Multi.createFrom().ticks().every(Duration.ofSeconds(10))
            .subscribe().with(tick -> cleanupStaleAgents());
    }
    
    private void cleanupStaleAgents() {
        Instant threshold = Instant.now().minus(HEARTBEAT_TIMEOUT);
        
        agents.entrySet().removeIf(entry -> {
            if (entry.getValue().lastHeartbeat().isBefore(threshold)) {
                LOG.warn("Removing stale agent: {}", entry.getKey());
                persistence.deleteAgent(entry.getKey()).subscribe().with(
                    v -> LOG.debug("Cleaned up agent: {}", entry.getKey()),
                    error -> LOG.error("Failed to cleanup agent", error)
                );
                return true;
            }
            return false;
        });
    }
}
