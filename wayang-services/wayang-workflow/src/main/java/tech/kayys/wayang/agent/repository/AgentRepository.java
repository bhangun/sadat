package tech.kayys.wayang.agent.repository;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.AgentDefinition;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ApplicationScoped
public class AgentRepository {

    private final Map<String, AgentDefinition> agents = new ConcurrentHashMap<>();

    public Uni<AgentDefinition> save(AgentDefinition agent) {
        agents.put(agent.getId(), agent);
        return Uni.createFrom().item(agent);
    }

    public Uni<AgentDefinition> findById(String id) {
        return Uni.createFrom().item(agents.get(id));
    }

    public Uni<List<AgentDefinition>> findAll(AgentDefinition.AgentStatus status,
            AgentDefinition.AgentType type) {
        return Uni.createFrom().item(
                agents.values().stream()
                        .filter(agent -> status == null || agent.getStatus() == status)
                        .filter(agent -> type == null || agent.getType() == type)
                        .collect(Collectors.toList()));
    }

    public Uni<Boolean> delete(String id) {
        return Uni.createFrom().item(agents.remove(id) != null);
    }

    public Uni<Boolean> exists(String id) {
        return Uni.createFrom().item(agents.containsKey(id));
    }
}