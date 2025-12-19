package tech.kayys.wayang.agent.repository;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.dto.AgentDefinition;

@ApplicationScoped
public class AgentDefinitionRepository {

    public Uni<AgentDefinition> saveAgent(AgentDefinition agent) {
        return Uni.createFrom().item(agent);
    }

    public Uni<AgentDefinition> updateAgent(AgentDefinition agent) {
        return Uni.createFrom().item(agent);
    }

    public Uni<AgentDefinition> findAgentById(String id) {
        return Uni.createFrom().nullItem();
    }
}
