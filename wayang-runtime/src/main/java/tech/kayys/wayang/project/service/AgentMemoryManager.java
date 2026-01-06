package tech.kayys.wayang.project.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.project.domain.AIAgent;

/**
 * Agent Memory Manager
 */
@ApplicationScoped
public class AgentMemoryManager {

    private static final Logger LOG = LoggerFactory.getLogger(AgentMemoryManager.class);

    public Uni<AgentMemory> initializeMemory(AIAgent agent) {
        LOG.info("Initializing memory for agent: {}", agent.agentId);

        return Uni.createFrom().item(() -> {
            if (agent.memoryConfig == null) {
                return new InMemoryAgentMemory();
            }

            // In production, create appropriate memory type
            return new InMemoryAgentMemory();
        });
    }

    public List<String> retrieveRelevant(AgentMemory memory, String query) {
        return memory.search(query, 5);
    }

    public void storeInteraction(
            AgentMemory memory,
            String instruction,
            String response) {

        String key = "interaction_" + Instant.now().toEpochMilli();
        String value = "Q: " + instruction + "\nA: " + response;
        memory.store(key, value);
    }
}

/**
 * In-memory agent memory implementation
 */
class InMemoryAgentMemory implements AgentMemory {

    private final Map<String, String> storage = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public void store(String key, String value) {
        storage.put(key, value);
    }

    @Override
    public String retrieve(String key) {
        return storage.get(key);
    }

    @Override
    public List<String> search(String query, int limit) {
        // Simple keyword search
        return storage.values().stream()
                .filter(value -> value.toLowerCase().contains(query.toLowerCase()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
