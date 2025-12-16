package tech.kayys.wayang.tool.service;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.model.Tool;
import tech.kayys.wayang.workflow.model.ExecutionContext;

import org.jboss.logging.Logger;

import java.util.Map;

/**
 * Tool Service - handles tool execution
 */
@ApplicationScoped
public class ToolService {

    private static final Logger LOG = Logger.getLogger(ToolService.class);

    @Inject
    Vertx vertx;

    private WebClient webClient;

    @jakarta.annotation.PostConstruct
    void init() {
        webClient = WebClient.create(vertx);
    }

    /**
     * Execute tool with parameters
     */
    public Uni<Map<String, Object>> executeTool(Tool tool, Map<String, Object> parameters,
            ExecutionContext context) {
        if (!tool.getEnabled()) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Tool is not enabled: " + tool.getName()));
        }

        LOG.debugf("Executing tool: %s (type: %s)", tool.getName(), tool.getType());

        switch (tool.getType()) {
            case API:
                return executeApiTool(tool, parameters);
            case FUNCTION:
                return executeFunctionTool(tool, parameters);
            case WEB_SEARCH:
                return executeWebSearchTool(tool, parameters);
            default:
                return Uni.createFrom().failure(
                        new UnsupportedOperationException("Tool type not supported: " + tool.getType()));
        }
    }

    /**
     * Execute API tool
     */
    private Uni<Map<String, Object>> executeApiTool(Tool tool, Map<String, Object> parameters) {
        String endpoint = tool.getConfig().getEndpoint();
        String method = tool.getConfig().getMethod();

        var request = switch (method.toUpperCase()) {
            case "GET" -> webClient.getAbs(endpoint);
            case "POST" -> webClient.postAbs(endpoint);
            case "PUT" -> webClient.putAbs(endpoint);
            case "DELETE" -> webClient.deleteAbs(endpoint);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };

        // Add headers
        if (tool.getConfig().getHeaders() != null) {
            tool.getConfig().getHeaders().forEach(request::putHeader);
        }

        // Add authentication
        addAuthentication(request, tool);

        // Send request
        if ("POST".equals(method) || "PUT".equals(method)) {
            return request.sendJsonObject(JsonObject.mapFrom(parameters))
                    .map(response -> response.bodyAsJsonObject().getMap());
        } else {
            return request.send()
                    .map(response -> response.bodyAsJsonObject().getMap());
        }
    }

    /**
     * Execute function tool (custom logic)
     */
    private Uni<Map<String, Object>> executeFunctionTool(Tool tool, Map<String, Object> parameters) {
        // Placeholder for custom function execution
        return Uni.createFrom().item(Map.of("result", "Function executed"));
    }

    /**
     * Execute web search tool
     */
    private Uni<Map<String, Object>> executeWebSearchTool(Tool tool, Map<String, Object> parameters) {
        // Placeholder for web search implementation
        String query = (String) parameters.get("query");
        return Uni.createFrom().item(Map.of(
                "results", "Search results for: " + query));
    }

    /**
     * Add authentication to request
     */
    private void addAuthentication(io.vertx.mutiny.ext.web.client.HttpRequest<?> request, Tool tool) {
        if (tool.getConfig().getAuthentication() == null) {
            return;
        }

        String authType = tool.getConfig().getAuthentication().getType();
        Map<String, Object> credentials = tool.getConfig().getAuthentication().getCredentials();

        switch (authType.toLowerCase()) {
            case "bearer":
                String token = (String) credentials.get("token");
                request.putHeader("Authorization", "Bearer " + token);
                break;
            case "apikey":
                String apiKey = (String) credentials.get("apiKey");
                String headerName = (String) credentials.getOrDefault("headerName", "X-API-Key");
                request.putHeader(headerName, apiKey);
                break;
            case "basic":
                String username = (String) credentials.get("username");
                String password = (String) credentials.get("password");
                String basicAuth = java.util.Base64.getEncoder()
                        .encodeToString((username + ":" + password).getBytes());
                request.putHeader("Authorization", "Basic " + basicAuth);
                break;
        }
    }
}
