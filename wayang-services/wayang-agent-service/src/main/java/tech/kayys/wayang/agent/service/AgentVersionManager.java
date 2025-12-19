package tech.kayys.wayang.agent.service;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.dto.UpdateAgentRequest;
import tech.kayys.wayang.agent.dto.AgentVersion;
import tech.kayys.wayang.agent.entity.AgentEntity;

@ApplicationScoped
public class AgentVersionManager {

    public Uni<AgentEntity> createNewVersion(AgentEntity existingAgent, UpdateAgentRequest request) {
        Log.infof("Creating new version for agent: %s", existingAgent.getId());

        // In a real implementation, this would create a new version of the agent
        // For now, we'll just update the existing agent
        AgentEntity updatedAgent = new AgentEntity();
        updatedAgent.setId(existingAgent.getId()); // Keep same ID
        updatedAgent.setName(request.name() != null ? request.name() : existingAgent.getName());
        updatedAgent.setDescription(request.description() != null ? request.description() : existingAgent.getDescription());
        updatedAgent.setTenantId(existingAgent.getTenantId());
        updatedAgent.setType(existingAgent.getType() != null ? existingAgent.getType() : request.agentType());
        updatedAgent.setLlmConfig(request.llmConfig() != null ? toJsonString(request.llmConfig()) : existingAgent.getLlmConfig());
        updatedAgent.setTools(request.tools() != null ? toJsonString(request.tools()) : existingAgent.getTools());
        updatedAgent.setConfig(request.config() != null ? toJsonString(request.config()) : existingAgent.getConfig());
        updatedAgent.setIsActive(request.isActive() != null ? request.isActive() : existingAgent.getIsActive());

        return Uni.createFrom().item(updatedAgent);
    }

    public Uni<java.util.List<AgentVersion>> getVersionHistory(String agentId) {
        Log.debugf("Retrieving version history for agent: %s", agentId);

        // Return a mock history
        AgentVersion currentVersion = new AgentVersion(
            "v1.0",
            System.currentTimeMillis(),
            "Initial version",
            "system"
        );

        return Uni.createFrom().item(java.util.Collections.singletonList(currentVersion));
    }

    private String toJsonString(Object obj) {
        return obj != null ? obj.toString() : "{}";
    }
}