package tech.kayys.agent.schema;

import com.fasterxml.jackson.databind.JsonNode;

public record ExtensionPoint(String namespace, JsonNode properties) {}
