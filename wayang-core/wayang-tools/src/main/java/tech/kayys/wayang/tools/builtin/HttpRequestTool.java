package tech.kayys.wayang.tools.builtin;

import tech.kayys.wayang.tools.mcp.*;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class HttpRequestTool implements MCPTool {
    
    @Inject
    Vertx vertx;
    
    private WebClient client;
    
    @PostConstruct
    void init() {
        this.client = WebClient.create(vertx);
    }
    
    @Override
    public ToolDescriptor descriptor() {
        return new ToolDescriptor(
            "http_request",
            "1.0.0",
            "Make HTTP requests to external APIs",
            "network",
            Map.of("timeout", 30000)
        );
    }
    
    @Override
    public JsonNode schema() {
        // JSON Schema for input validation
        return JsonUtil.parseSchema("""
            {
              "type": "object",
              "required": ["url", "method"],
              "properties": {
                "url": {"type": "string", "format": "uri"},
                "method": {"type": "string", "enum": ["GET","POST","PUT","DELETE"]},
                "headers": {"type": "object"},
                "body": {}
              }
            }
            """);
    }
    
    @Override
    public Uni<ToolResponse> execute(ToolRequest request) {
        String url = (String) request.parameters().get("url");
        String method = (String) request.parameters().get("method");
        
        var httpRequest = client.requestAbs(
            io.vertx.core.http.HttpMethod.valueOf(method), 
            url
        );
        
        // Add headers
        Map<String, Object> headers = (Map<String, Object>) 
            request.parameters().getOrDefault("headers", Map.of());
        headers.forEach((k, v) -> httpRequest.putHeader(k, v.toString()));
        
        return httpRequest.sendJson(request.parameters().get("body"))
            .map(response -> ToolResponse.success(
                request.requestId(),
                Map.of(
                    "statusCode", response.statusCode(),
                    "headers", response.headers().names(),
                    "body", response.bodyAsString()
                )
            ))
            .onFailure().recoverWithItem(failure -> 
                ToolResponse.error(ErrorPayload.toolError(failure))
            );
    }
    
    @Override
    public List<String> requiredSecrets() {
        return List.of(); // No secrets required for basic HTTP
    }
    
    @Override
    public Set<ToolCapability> capabilities() {
        return Set.of(ToolCapability.NETWORK_ACCESS);
    }
}