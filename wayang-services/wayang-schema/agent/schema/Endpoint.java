package tech.kayys.agent.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.net.URI;

public record Endpoint(
    URI uri,
    String transport,   // "http", "grpc", "websocket", "mcp"
    JsonNode options
) {
    public Endpoint {
        if (uri == null) {
            throw new IllegalArgumentException("uri must not be null");
        }
        if (transport == null || transport.isBlank()) {
            throw new IllegalArgumentException("transport must not be blank");
        }
        options = (options == null) ? JsonNodeFactory.instance.objectNode() : options;
    }
}