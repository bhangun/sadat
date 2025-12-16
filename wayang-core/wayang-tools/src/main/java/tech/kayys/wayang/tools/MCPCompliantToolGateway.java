
@ApplicationScoped
public class MCPCompliantToolGateway implements ToolGateway {
    @Inject ToolRegistry toolRegistry;
    @Inject ToolValidator toolValidator;
    @Inject ToolExecutor toolExecutor;
    @Inject SecretManager secretManager;
    @Inject RateLimiter rateLimiter;
    @Inject CircuitBreakerRegistry circuitBreakerRegistry;
    @Inject ToolAuditor toolAuditor;
    
    @Override
    public CompletableFuture<ToolResult> execute(ToolRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            Span span = startSpan(request);
            
            try {
                // Get tool descriptor
                ToolDescriptor tool = toolRegistry.getTool(request.getToolId())
                    .orElseThrow(() -> new ToolNotFoundException(request.getToolId()));
                
                // Validate request
                ValidationResult validation = toolValidator.validate(request, tool);
                if (!validation.isValid()) {
                    throw new ToolValidationException(validation.getErrors());
                }
                
                // Check rate limit
                if (!rateLimiter.allowRequest(request.getTenantId(), tool.getId())) {
                    throw new RateLimitExceededException();
                }
                
                // Get circuit breaker
                CircuitBreaker circuitBreaker = circuitBreakerRegistry.get(tool.getId());
                
                // Inject secrets
                ToolRequest enriched = injectSecrets(request, tool);
                
                // Execute with circuit breaker
                ToolResult result = circuitBreaker.executeSupplier(
                    () -> toolExecutor.execute(enriched, tool)
                );
                
                // Audit
                toolAuditor.audit(request, result);
                
                return result;
                
            } finally {
                span.end();
            }
        });
    }
    
    private ToolRequest injectSecrets(ToolRequest request, ToolDescriptor tool) {
        if (tool.getRequiredSecrets().isEmpty()) {
            return request;
        }
        
        Map<String, Object> parameters = new HashMap<>(request.getParameters());
        
        for (String secretKey : tool.getRequiredSecrets()) {
            String secret = secretManager.getSecret(
                request.getTenantId(),
                secretKey
            );
            parameters.put(secretKey, secret);
        }
        
        return request.withParameters(parameters);
    }
}
