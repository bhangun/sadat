
// tech.kayys.platform.executor.HttpToolExecutor.java
package tech.kayys.platform.executor;

import tech.kayys.platform.schema.*;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class HttpToolExecutor implements AgentExecutor {
    private final AgentRegistry registry;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final Map<URI, List<URI>> specServerCache; // specUrl → servers

    public HttpToolExecutor(
        AgentRegistry registry,
        ObjectMapper mapper,
        Map<URI, List<URI>> specServerCache
    ) {
        this.registry = registry;
        this.mapper = mapper;
        this.specServerCache = specServerCache;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public boolean supports(AgentCapability capability) {
        return capability.id().startsWith("openapi.");
    }

    @Override
    public JsonNode execute(
        AgentDefinition agent,
        AgentCapability capability,
        Map<String, JsonNode> input
    ) throws Exception {
        // Resolve which OpenAPI spec this tool belongs to
        // (In real system: store specUrl in ToolDefinition.extensions)
        URI specUrl = extractSpecUrl(capability.id());
        List<URI> servers = specServerCache.get(specUrl);
        if (servers == null || servers.isEmpty()) {
            throw new IllegalStateException("No servers found for spec: " + specUrl);
        }

        // Build HTTP request (simplified: assumes first server, POST with body)
        URI target = servers.get(0).resolve("/" + extractPathFromToolId(capability.id()));
        String jsonBody = mapper.writeValueAsString(input.get("body"));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(target)
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(response.body());
    }

    // In production: store specUrl in ToolDefinition via ExtensionPoint
    private URI extractSpecUrl(String toolId) {
        // e.g., "openapi.example.com.get_user" → https://example.com/openapi.json
        String host = toolId.split("\\.")[1];
        return URI.create("https://" + host + "/openapi.json");
    }

    private String extractPathFromToolId(String toolId) {
        // Reverse of sanitize() — in real system, store path in ToolDefinition
        return toolId.substring(toolId.lastIndexOf(".") + 1).replace("_", "/");
    }
}