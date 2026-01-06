
/**
 * ==============================================
 * TOOL GATEWAY CLIENT - MCP Tool Execution
 * ==============================================
 */
@ApplicationScoped
public class ToolGatewayClient {

    @Inject
    @RestClient
    ToolGatewayService toolService;

    @Inject
    SecretManager secretManager;

    @Inject
    SchemaValidator schemaValidator;

    @Inject
    CircuitBreakerManager circuitBreaker;

    /**
     * Execute tool with validation and security
     */
    public Uni<ToolResponse> execute(ToolRequest request) {
        return Uni.createFrom().item(() -> {
            // Get tool definition
            return toolService.getToolDefinition(request.getTool());
        })
            .flatMap(toolDef -> {
                // Validate parameters against schema
                var errors = schemaValidator.validate(
                    request.getParameters(),
                    toolDef.getParameterSchema()
                );

                if(!errors.isEmpty()) {
            return Uni.createFrom().item(
                ToolResponse.error("Parameter validation failed: " + errors)
            );
        }

        // Get ephemeral credentials if needed
        return getCredentials(toolDef)
            .flatMap(credentials -> {
                // Execute with circuit breaker
                return circuitBreaker.execute(
                    "tool." + request.getTool(),
                    () -> executeWithTimeout(request, toolDef, credentials)
                );
            });
    })
        .onFailure().recoverWithItem(throwable -> {
    // Convert failure to structured response
    var isRetryable = throwable instanceof TimeoutException ||
        throwable instanceof IOException;

    return ToolResponse.builder()
        .error(throwable.getMessage())
        .retryable(isRetryable)
        .build();
});
    }
    
    private Uni < Map < String, String >> getCredentials(ToolDefinition toolDef) {
    if (toolDef.getRequiredSecrets() == null || toolDef.getRequiredSecrets().isEmpty()) {
        return Uni.createFrom().item(Map.of());
    }

    var credentialUnis = toolDef.getRequiredSecrets().stream()
        .map(secretKey -> secretManager.getSecret(secretKey)
            .map(secret -> Map.entry(secretKey, secret)))
        .collect(Collectors.toList());

    return Uni.join().all(credentialUnis).andFailFast()
        .map(entries -> entries.stream()
            .collect(Collectors.toMap(Map.Entry:: getKey, Map.Entry:: getValue)));
}
    
    private Uni < ToolResponse > executeWithTimeout(
    ToolRequest request,
    ToolDefinition toolDef,
    Map < String, String > credentials
) {
    var executionRequest = new ToolExecutionRequest();
    executionRequest.setTool(request.getTool());
    executionRequest.setParameters(request.getParameters());
    executionRequest.setCredentials(credentials);
    executionRequest.setRunId(request.getRunId());
    executionRequest.setTenantId(request.getTenantId());

    return toolService.executeTool(executionRequest)
        .ifNoItem().after(Duration.ofMillis(request.getTimeout()))
        .fail()
        .map(apiResponse -> {
            var response = new ToolResponse();
            response.setResult(apiResponse.getResult());
            response.setDuration(apiResponse.getDuration());
            response.setError(apiResponse.getError());
            response.setRetryable(apiResponse.isRetryable());
            return response;
        });
}
}
