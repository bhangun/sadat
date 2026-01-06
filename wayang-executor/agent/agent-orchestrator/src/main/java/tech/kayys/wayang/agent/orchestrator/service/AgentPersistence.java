package tech.kayys.wayang.agent.orchestrator.service;

import java.time.Instant;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.dto.AgentRegistration;
import tech.kayys.wayang.agent.dto.AgentStatus;

/**
 * Agent persistence layer
 */
@ApplicationScoped
public class AgentPersistence {
    
    public Uni<Void> saveAgent(AgentRegistration agent) {
        // Persist to database
        return Uni.createFrom().voidItem();
    }
    
    public Uni<Void> deleteAgent(String agentId) {
        return Uni.createFrom().voidItem();
    }
    
    public Uni<Void> updateHeartbeat(String agentId, Instant timestamp) {
        return Uni.createFrom().voidItem();
    }
    
    public Uni<Void> updateStatus(String agentId, AgentStatus status) {
        return Uni.createFrom().voidItem();
    }
}