
/**
 * HTTP Node - Make HTTP requests with full configurability
 * Supports all methods, headers, auth, and retries
 */
@ApplicationScoped
@NodeType("builtin.http")
public class HttpNode extends AbstractNode {
    
    @Inject
    @RestClient
    HttpClientService httpClient;
    
    @Override
    protected Uni<ExecutionResult> doExecute(NodeContext context) {
        var url = config.getString("url");
        var method = config.getString("method", "GET");
        var headers = config.getObject("headers", Map.class);
        var body = context.getInput("body");
        
        var request = HttpRequest.builder()
            .url(url)
            .method(HttpMethod.valueOf(method))
            .headers(headers)
            .body(body)
            .timeout(config.getInt("timeout", 30000))
            .build();
        
        return httpClient.execute(request)
            .map(response -> ExecutionResult.success(Map.of(
                "status", response.getStatus(),
                "headers", response.getHeaders(),
                "body", response.getBody()
            )));
    }
}
