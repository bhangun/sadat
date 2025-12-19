package tech.kayys.agent.schema;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public record CapabilityProfile(
    String strategy,    // "llm", "code", "human", "mcp", "workflow"
    JsonNode hints
) {
    public CapabilityProfile {
        if (strategy == null || strategy.isBlank()) {
            throw new IllegalArgumentException("strategy must not be blank");
        }
        hints = (hints == null) ? JsonNodeFactory.instance.objectNode() : hints;
    }

    public static CapabilityProfile llm(String model) {
        return new CapabilityProfile("llm", 
            JsonNodeFactory.instance.objectNode().put("model", model));
    }

    public static CapabilityProfile mcp() {
        return new CapabilityProfile("mcp", JsonNodeFactory.instance.objectNode());
    }
}