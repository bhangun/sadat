package tech.kayys.wayang.node.executor;

/**
 * WEBHOOK node executor
 */
@ApplicationScoped
public class WebhookNodeExecutor implements NodeExecutor {

    private static final Logger LOG = Logger.getLogger(WebhookNodeExecutor.class);

    @Inject
    io.vertx.mutiny.core.Vertx vertx;

    private io.vertx.mutiny.ext.web.client.WebClient webClient;

    @jakarta.annotation.PostConstruct
    void init() {
        webClient = io.vertx.mutiny.ext.web.client.WebClient.create(vertx);
    }

    @Override
    public Uni<NodeExecutionResult> execute(Workflow.Node node, ExecutionContext context) {
        // Webhook config should be in tool config
        if (node.getConfig().getTool() == null) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Webhook requires tool configuration"));
        }

        io.quarkus.ai.agent.runtime.model.Tool tool = node.getConfig().getTool();
        String url = tool.getConfig().getEndpoint();
        String method = tool.getConfig().getMethod() != null ? tool.getConfig().getMethod() : "POST";

        Map<String, Object> payload = context.getAllVariables();

        LOG.infof("Triggering webhook: %s %s", method, url);

        var request = switch (method.toUpperCase()) {
            case "GET" -> webClient.getAbs(url);
            case "POST" -> webClient.postAbs(url);
            case "PUT" -> webClient.putAbs(url);
            case "DELETE" -> webClient.deleteAbs(url);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };

        // Add headers
        if (tool.getConfig().getHeaders() != null) {
            tool.getConfig().getHeaders().forEach(request::putHeader);
        }

        // Add authentication
        addAuthentication(request, tool);

        // Set timeout
        int timeout = tool.getConfig().getTimeout() != null ? tool.getConfig().getTimeout() : 30000;
        request.timeout(timeout);

        Uni<io.vertx.mutiny.ext.web.client.HttpResponse<io.vertx.mutiny.core.buffer.Buffer>> responseUni;

        if ("POST".equals(method) || "PUT".equals(method)) {
            responseUni = request.sendJsonObject(
                    io.vertx.core.json.JsonObject.mapFrom(payload));
        } else {
            responseUni = request.send();
        }

        return responseUni.map(response -> {
            Map<String, Object> output = new HashMap<>();
            output.put("statusCode", response.statusCode());
            output.put("body", response.bodyAsString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                try {
                    output.put("json", response.bodyAsJsonObject().getMap());
                } catch (Exception e) {
                    // Not JSON, that's okay
                }
            }

            return new NodeExecutionResult(node.getId(), true, output, null);
        });
    }

    private void addAuthentication(
            io.vertx.mutiny.ext.web.client.HttpRequest<?> request,
            io.quarkus.ai.agent.runtime.model.Tool tool) {

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

    @Override
    public NodeType getSupportedType() {
        return Workflow.Node.NodeType.WEBHOOK;
    }
}