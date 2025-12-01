package tech.kayys.agent.schema;

public record CustomJsonExtension(String namespace, JsonNode payload)
    implements AgentExtension {}
