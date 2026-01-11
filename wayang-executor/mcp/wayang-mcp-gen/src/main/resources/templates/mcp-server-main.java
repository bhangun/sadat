package {model.packageName};

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.quarkus.logging.Log;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
* {model.title}
* {model.description}
*
* Generated MCP Server from OpenAPI specification
* Version: {model.version}
* Package: {model.packageName}
*/
@ApplicationScoped
public class {model.serverClass} extends AbstractVerticle {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, McpTool> tools = new HashMap<>();

    @Override
    public void start(Promise<Void> startPromise) {
        Log.info("Starting {model.title} v{model.version}");

        registerTools();
        setupServer(startPromise);
    }

    private void registerTools() {
        Log.info("Registering {} MCP tools", {model.tools.size()});
{#for tool in model.tools}
        tools.put("{tool.name}", new {tool.className}());
{/for}
        Log.info("Successfully registered all tools");
    }

    private void setupServer(Promise<Void> startPromise) {
        Router router = Router.router(vertx);

        // Enable CORS
        CorsHandler corsHandler = CorsHandler.create("*")
            .allowedMethods(Set.of(HttpMethod.GET, HttpMethod.POST, HttpMethod.OPTIONS))
            .allowedHeaders(Set.of("Content-Type", "Authorization"));
        router.route().handler(corsHandler);

        // Enable body parsing
        router.route().handler(BodyHandler.create());

        // MCP endpoint
        router.post("/mcp").handler(ctx -> {
            try {
                String body = ctx.getBodyAsString();
                JsonNode requestJson = objectMapper.readTree(body);

                handleMcpRequest(requestJson).thenAccept(response -> {
                    ctx.response()
                        .putHeader("Content-Type", "application/json")
                        .end(response.toString());
                }).exceptionally(throwable -> {
                    Log.error("Error handling MCP request", throwable);
                    ObjectNode errorResponse = createErrorResponse(
                        "Internal error: " + throwable.getMessage());
                    ctx.response()
                        .setStatusCode(500)
                        .putHeader("Content-Type", "application/json")
                        .end(errorResponse.toString());
                    return null;
                });
            } catch (Exception e) {
                Log.error("Error parsing MCP request", e);
                ObjectNode errorResponse = createErrorResponse("Invalid JSON request: " + e.getMessage());
                ctx.response()
                    .setStatusCode(400)
                    .putHeader("Content-Type", "application/json")
                    .end(errorResponse.toString());
            }
        });

        // Health check endpoint
        router.get("/health").handler(ctx -> {
            ObjectNode health = objectMapper.createObjectNode();
            health.put("status", "UP");
            health.put("server", "{model.title}");
            health.put("version", "{model.version}");
            health.put("toolsRegistered", tools.size());
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(health.toString());
        });

        // Server info endpoint
        router.get("/info").handler(ctx -> {
            ObjectNode info = objectMapper.createObjectNode();
            info.put("name", "{model.title}");
            info.put("description", "{model.description}");
            info.put("version", "{model.version}");
            info.put("protocol", "MCP");
            info.put("protocolVersion", "2024-11-05");
            ArrayNode toolsArray = objectMapper.createArrayNode();
            tools.keySet().forEach(toolsArray::add);
            info.set("availableTools", toolsArray);
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end(info.toString());
        });

        // Create HTTP server
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080)
            .onSuccess(server -> {
                Log.info("{model.title} started successfully on port 8080");
                Log.info("MCP endpoint available at: http://localhost:8080/mcp");
                Log.info("Health check at: http://localhost:8080/health");
                startPromise.complete();
            })
            .onFailure(error -> {
                Log.error("Failed to start server", error);
                startPromise.fail(error);
            });
    }

    private CompletableFuture<ObjectNode> handleMcpRequest(JsonNode request) {
        String method = request.has("method") ? request.get("method").asText() : "";
        String id = request.has("id") ? request.get("id").asText() : null;

        Log.debug("Handling MCP request: method={}, id={}", method, id);

        switch (method) {
            case "initialize":
                return CompletableFuture.completedFuture(handleInitialize(request));
            case "tools/list":
                return CompletableFuture.completedFuture(handleToolsList(request));
            case "tools/call":
                return handleToolCall(request);
            case "ping":
                return CompletableFuture.completedFuture(handlePing(request));
            default:
                return CompletableFuture.completedFuture(createErrorResponse(
                    "Unknown method: " + method, -32601, id));
        }
    }

    private ObjectNode handleInitialize(JsonNode request) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (request.has("id")) {
            response.set("id", request.get("id"));
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", "2024-11-05");

        ObjectNode capabilities = objectMapper.createObjectNode();
        ObjectNode toolsCapability = objectMapper.createObjectNode();
        toolsCapability.put("listChanged", true);
        capabilities.set("tools", toolsCapability);
        result.set("capabilities", capabilities);

        ObjectNode serverInfo = objectMapper.createObjectNode();
        serverInfo.put("name", "{model.title}");
        serverInfo.put("version", "{model.version}");
        result.set("serverInfo", serverInfo);

        response.set("result", result);
        Log.info("MCP session initialized");
        return response;
    }

    private ObjectNode handleToolsList(JsonNode request) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (request.has("id")) {
            response.set("id", request.get("id"));
        }

        ArrayNode toolsArray = objectMapper.createArrayNode();
        for (Map.Entry<String, McpTool> entry : tools.entrySet()) {
            ObjectNode toolDef = objectMapper.createObjectNode();
            toolDef.put("name", entry.getKey());
            toolDef.set("inputSchema", entry.getValue().getInputSchema());
            toolsArray.add(toolDef);
        }

        ObjectNode result = objectMapper.createObjectNode();
        result.set("tools", toolsArray);
        response.set("result", result);

        Log.debug("Returning {} tools", toolsArray.size());
        return response;
    }

    private CompletableFuture<ObjectNode> handleToolCall(JsonNode request) {
        String toolName = request.get("params").get("name").asText();
        JsonNode arguments = request.get("params").get("arguments");

        Log.debug("Calling tool: {} with arguments: {}", toolName, arguments);

        McpTool tool = tools.get(toolName);
        if (tool == null) {
            return CompletableFuture.completedFuture(createErrorResponse(
                "Unknown tool: " + toolName, -32602, request.get("id").asText()));
        }

        CompletableFuture<ObjectNode> future = new CompletableFuture<>();
        try {
            tool.execute(arguments).thenAccept(result -> {
                ObjectNode response = objectMapper.createObjectNode();
                response.put("jsonrpc", "2.0");
                response.set("id", request.get("id"));
                ObjectNode resultObj = objectMapper.createObjectNode();
                resultObj.set("content", result);
                response.set("result", resultObj);
                future.complete(response);
            }).exceptionally(throwable -> {
                Log.error("Tool execution failed", throwable);
                future.complete(createErrorResponse(
                    "Tool execution failed: " + throwable.getMessage(), -32603, 
                    request.get("id").asText()));
                return null;
            });
        } catch (Exception e) {
            Log.error("Error calling tool", e);
            future.complete(createErrorResponse(
                "Tool call failed: " + e.getMessage(), -32603, 
                request.get("id").asText()));
        }

        return future;
    }

    private ObjectNode handlePing(JsonNode request) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (request.has("id")) {
            response.set("id", request.get("id"));
        }
        response.put("result", "pong");
        return response;
    }

    private ObjectNode createErrorResponse(String message, int code, String id) {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id != null) {
            response.put("id", id);
        }
        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        response.set("error", error);
        return response;
    }

    private ObjectNode createErrorResponse(String message) {
        return createErrorResponse(message, -32603, null);
    }
}