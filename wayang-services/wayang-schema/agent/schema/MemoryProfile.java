package tech.kayys.agent.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public record MemoryProfile(
    String behavior,    // "conversation", "retrieval", "graph"
    int contextWindow,
    JsonNode hints
) {
    public MemoryProfile {
        if (behavior == null || behavior.isBlank()) {
            throw new IllegalArgumentException("behavior must not be blank");
        }
        if (contextWindow <= 0) {
            throw new IllegalArgumentException("contextWindow must be > 0");
        }
        hints = (hints == null) ? JsonNodeFactory.instance.objectNode() : hints;
    }
}