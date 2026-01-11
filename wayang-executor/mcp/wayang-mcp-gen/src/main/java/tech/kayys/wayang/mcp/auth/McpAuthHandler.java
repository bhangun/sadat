package tech.kayys.wayang.mcp.auth;

import com.fasterxml.jackson.databind.JsonNode;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Base64;
import java.util.Optional;

/**
 * Authentication handler for MCP server
 * Handles various authentication schemes defined in OpenAPI spec
 */
@ApplicationScoped
public class McpAuthHandler {

    public Optional<String> extractBearerToken(JsonNode arguments) {
        if (arguments.has("authorization")) {
            String auth = arguments.get("authorization").asText();
            if (auth.toLowerCase().startsWith("bearer ")) {
                return Optional.of(auth.substring(7));
            }
        }

        if (arguments.has("token")) {
            return Optional.of(arguments.get("token").asText());
        }

        // Check environment variable
        String envToken = System.getenv("API_TOKEN");
        if (envToken != null && !envToken.isEmpty()) {
            return Optional.of(envToken);
        }

        return Optional.empty();
    }

    public Optional<String> extractApiKey(JsonNode arguments) {
        if (arguments.has("apiKey")) {
            return Optional.of(arguments.get("apiKey").asText());
        }

        if (arguments.has("api_key")) {
            return Optional.of(arguments.get("api_key").asText());
        }

        // Check environment variable
        String envApiKey = System.getenv("API_KEY");
        if (envApiKey != null && !envApiKey.isEmpty()) {
            return Optional.of(envApiKey);
        }

        return Optional.empty();
    }

    public Optional<String> extractBasicAuth(JsonNode arguments) {
        if (arguments.has("username") && arguments.has("password")) {
            String username = arguments.get("username").asText();
            String password = arguments.get("password").asText();
            String credentials = username + ":" + password;
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes());
            return Optional.of("Basic " + encoded);
        }

        return Optional.empty();
    }

    public boolean validateToken(String token) {
        // Implement token validation logic here
        // This is a placeholder implementation
        Log.debugf("Validating token: %s", token != null ? "***" : "null");
        return token != null && !token.trim().isEmpty();
    }

    public boolean validateApiKey(String apiKey) {
        // Implement API key validation logic here
        // This is a placeholder implementation
        Log.debugf("Validating API key: %s", apiKey != null ? "***" : "null");
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}