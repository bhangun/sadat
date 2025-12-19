package tech.kayys.agent.schema;

import com.fasterxml.jackson.databind.JsonNode;


/**
 * Structured, namespaced extension for future protocols or domains.
 * Example: namespace = "mcp.kayys.tech", properties = {"version": "0.3.0"}
 */
public record ExtensionPoint(
    String namespace,
    JsonNode properties
) {
    public ExtensionPoint {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("namespace must not be blank");
        }
        if (properties == null) {
            throw new IllegalArgumentException("properties must not be null");
        }
    }
}