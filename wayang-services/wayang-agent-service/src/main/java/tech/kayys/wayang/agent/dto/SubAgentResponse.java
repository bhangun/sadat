package tech.kayys.wayang.agent.dto;

import java.util.Map;

public record SubAgentResponse(
        String subAgentId,
        String orchestratorId,
        String status,
        Map<String, Object> config) {
    
    public static SubAgentResponse from(Object subAgent, Object request) {
        return new SubAgentResponse(
            "placeholder-subagent-id",
            "placeholder-orchestrator-id", 
            "ACTIVE",
            Map.of()
        );
    }
}