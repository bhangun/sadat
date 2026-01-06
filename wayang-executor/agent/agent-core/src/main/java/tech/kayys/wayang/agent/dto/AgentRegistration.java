package tech.kayys.wayang.agent.dto;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import tech.kayys.wayang.agent.AgentCapability;
import tech.kayys.wayang.agent.AgentType;

/**
 * Agent Registration
 */
public record AgentRegistration(
    String agentId,
    String agentName,
    AgentType agentType,
    Set<AgentCapability> capabilities,
    AgentStatus status,
    AgentEndpoint endpoint,
    Map<String, String> metadata,
    Instant registeredAt,
    Instant lastHeartbeat
) {
    
    public boolean isAvailable() {
        return status == AgentStatus.AVAILABLE;
    }
    
    public boolean isHealthy() {
        // Agent is healthy if heartbeat within last 30 seconds
        return lastHeartbeat.isAfter(Instant.now().minusSeconds(30));
    }
}