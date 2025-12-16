
/**
 * Tool gateway - secure proxy for tool execution
 */
@ApplicationScoped
public class ToolGateway {
    private final Map<String, ToolExecutor> executors;
    private final ToolValidator validator;
    private final SecretManager secretManager;
    private final RateLimiter rateLimiter;
    private final CircuitBreaker circuitBreaker;
    private final AuditLogger auditLogger;
    
    /**
     * Execute tool with security and reliability
     */
    public ToolResponse execute(ToolRequest request) throws ToolExecutionException {
        // Validate request
        ValidationResult validation = validator.validate(request);
        if (!validation.isValid()) {
            throw new ToolExecutionException("Invalid tool request: " + validation.getErrors());
        }
        
        // Check rate limit
        if (!rateLimiter.tryAcquire(request.getToolId())) {
            throw new ToolExecutionException("Rate limit exceeded");
        }
        
        // Inject secrets
        ToolRequest enrichedRequest = secretManager.injectSecrets(request);
        
        // Execute with circuit breaker
        ToolResponse response = circuitBreaker.call(() -> {
            ToolExecutor executor = getExecutor(request.getToolId());
            return executor.execute(enrichedRequest);
        });
        
        // Audit
        auditLogger.logToolExecution(request, response);
        
        return response;
    }
    
    private ToolExecutor getExecutor(String toolId) {
        return Optional.ofNullable(executors.get(toolId))
                      .orElseThrow(() -> new ToolExecutionException("Tool not found: " + toolId));
    }
}