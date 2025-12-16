

/**
 * Orchestrates inference requests across multiple runners with
 * intelligent routing, fallback, and load balancing.
 */
@ApplicationScoped
public class InferenceOrchestrator {
    
    private final ModelRouterService router;
    private final ModelRunnerFactory factory;
    private final ModelRepository repository;
    private final MetricsPublisher metrics;
    private final CircuitBreaker circuitBreaker;
    
    @Inject
    public InferenceOrchestrator(
        ModelRouterService router,
        ModelRunnerFactory factory,
        ModelRepository repository,
        MetricsPublisher metrics,
        CircuitBreaker circuitBreaker
    ) {
        this.router = router;
        this.factory = factory;
        this.repository = repository;
        this.metrics = metrics;
        this.circuitBreaker = circuitBreaker;
    }
    
    /**
     * Execute inference with automatic runner selection and fallback
     */
    public InferenceResponse execute(
        String modelId,
        InferenceRequest request,
        TenantContext tenantContext
    ) {
        var span = Span.current();
        span.setAttribute("model.id", modelId);
        span.setAttribute("tenant.id", tenantContext.tenantId().value());
        
        // Load model manifest
        ModelManifest manifest = repository
            .findById(modelId, tenantContext.tenantId())
            .orElseThrow(() -> new ModelNotFoundException(modelId));
        
        // Build request context with timeout and priority
        RequestContext ctx = RequestContext.builder()
            .tenantContext(tenantContext)
            .timeout(Duration.ofSeconds(30))
            .priority(request.priority())
            .preferredDevice(request.deviceHint())
            .build();
        
        // Select and rank candidate runners
        List<RunnerCandidate> candidates = router.selectRunners(
            manifest, 
            ctx
        );
        
        InferenceException lastError = null;
        
        // Attempt inference with fallback
        for (RunnerCandidate candidate : candidates) {
            try {
                return executeWithRunner(
                    manifest, 
                    candidate, 
                    request, 
                    ctx
                );
            } catch (InferenceException e) {
                lastError = e;
                metrics.recordFailure(
                    candidate.runnerName(), 
                    modelId, 
                    e.getClass().getSimpleName()
                );
                
                // Don't retry on quota or validation errors
                if (e instanceof TenantQuotaExceededException ||
                    e instanceof ValidationException) {
                    throw e;
                }
                
                span.addEvent("Runner failed, attempting fallback", 
                    Attributes.of(
                        AttributeKey.stringKey("runner"), candidate.runnerName(),
                        AttributeKey.stringKey("error"), e.getMessage()
                    ));
            }
        }
        
        throw new AllRunnersFailedException(
            "All runners failed for model " + modelId, 
            lastError
        );
    }
    
    private InferenceResponse executeWithRunner(
        ModelManifest manifest,
        RunnerCandidate candidate,
        InferenceRequest request,
        RequestContext ctx
    ) {
        var timer = metrics.startTimer();
        
        try {
            // Get or create runner instance
            ModelRunner runner = factory.getRunner(
                manifest, 
                candidate.runnerName(),
                ctx.tenantContext()
            );
            
            // Execute with circuit breaker protection
            InferenceResponse response = circuitBreaker.call(
                () -> runner.infer(request, ctx)
            );
            
            metrics.recordSuccess(
                candidate.runnerName(), 
                manifest.modelId(), 
                timer.stop()
            );
            
            return response;
            
        } catch (Exception e) {
            metrics.recordFailure(
                candidate.runnerName(), 
                manifest.modelId(), 
                e.getClass().getSimpleName()
            );
            throw new InferenceException(
                "Inference failed with runner: " + candidate.runnerName(), 
                e
            );
        }
    }
}