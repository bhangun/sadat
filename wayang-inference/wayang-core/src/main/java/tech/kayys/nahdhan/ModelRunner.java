
/**
 * Core SPI for model execution backends.
 * All adapters must implement this interface.
 */
public interface ModelRunner extends AutoCloseable {
    
    /**
     * Initialize the runner with model manifest and configuration
     * @param manifest Model metadata and artifact locations
     * @param config Runner-specific configuration
     * @param tenantContext Current tenant context for isolation
     * @throws ModelLoadException if initialization fails
     */
    void initialize(
        ModelManifest manifest, 
        Map<String, Object> config,
        TenantContext tenantContext
    ) throws ModelLoadException;
    
    /**
     * Execute synchronous inference
     * @param request Inference request with inputs
     * @param context Request context with timeout, priority, etc.
     * @return Inference response with outputs
     * @throws InferenceException if execution fails
     */
    InferenceResponse infer(
        InferenceRequest request,
        RequestContext context
    ) throws InferenceException;
    
    /**
     * Execute asynchronous inference with callback
     * @param request Inference request
     * @param context Request context
     * @return CompletionStage for async processing
     */
    CompletionStage<InferenceResponse> inferAsync(
        InferenceRequest request,
        RequestContext context
    );
    
    /**
     * Health check for this runner instance
     * @return Health status with diagnostics
     */
    HealthStatus health();
    
    /**
     * Get current resource utilization metrics
     * @return Resource usage snapshot
     */
    ResourceMetrics getMetrics();
    
    /**
     * Warm up the model (optional optimization)
     * @param sampleInputs Sample inputs for warming
     */
    default void warmup(List<InferenceRequest> sampleInputs) {
        // Default no-op
    }
    
    /**
     * Get runner metadata
     * @return Metadata about this runner implementation
     */
    RunnerMetadata metadata();
    
    /**
     * Gracefully release resources
     */
    @Override
    void close();
}