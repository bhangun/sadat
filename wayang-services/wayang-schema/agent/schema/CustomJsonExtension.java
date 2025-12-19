package tech.kayys.agent.schema;

import com.fasterxml.jackson.databind.JsonNode;

public record CustomJsonExtension(String namespace, JsonNode payload)
    implements AgentExtension {}
