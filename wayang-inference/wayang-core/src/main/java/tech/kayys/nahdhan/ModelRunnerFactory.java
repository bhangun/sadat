


/**
 * Factory for creating and managing runner instances with
 * warm pool, lifecycle management, and tenant isolation.
 */
@ApplicationScoped
public class ModelRunnerFactory {
    
    private static final int MAX_POOL_SIZE = 10;
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(15);
    
    @Inject
    Instance<ModelRunner> runnerProviders;
    
    @Inject
    ModelRepository repository;
    
    @Inject
    TenantConfig tenantConfig;
    
    // Pool: (tenantId, modelId, runnerName) -> Runner instance
    private final LoadingCache<RunnerCacheKey, ModelRunner> warmPool;
    
    // Track usage per runner for cleanup
    private final Map<RunnerCacheKey, Instant> lastAccess;
    
    @Inject
    public ModelRunnerFactory() {
        this.warmPool = Caffeine.newBuilder()
            .maximumSize(MAX_POOL_SIZE)
            .expireAfterAccess(IDLE_TIMEOUT)
            .removalListener(this::onRunnerEvicted)
            .build(this::createRunner);
            
        this.lastAccess = new ConcurrentHashMap<>();
        
        // Start cleanup scheduler
        startCleanupScheduler();
    }
    
    /**
     * Get or create runner instance for tenant
     */
    public ModelRunner getRunner(
        ModelManifest manifest,
        String runnerName,
        TenantContext tenantContext
    ) {
        RunnerCacheKey key = new RunnerCacheKey(
            tenantContext.tenantId(),
            manifest.modelId(),
            runnerName
        );
        
        // Update last access time
        lastAccess.put(key, Instant.now());
        
        // Get from pool or create
        return warmPool.get(key);
    }
    
    /**
     * Prewarm runners for specific models
     */
    public void prewarm(
        ModelManifest manifest,
        List<String> runnerNames,
        TenantContext tenantContext
    ) {
        runnerNames.forEach(runnerName -> {
            try {
                getRunner(manifest, runnerName, tenantContext);
            } catch (Exception e) {
                // Log but don't fail prewarming
                Log.warnf("Failed to prewarm runner %s: %s", 
                    runnerName, e.getMessage());
            }
        });
    }
    
    /**
     * Create new runner instance (called by cache loader)
     */
    private ModelRunner createRunner(RunnerCacheKey key) {
        // Load manifest
        ModelManifest manifest = repository
            .findById(key.modelId(), key.tenantId())
            .orElseThrow(() -> new ModelNotFoundException(key.modelId()));
        
        // Find runner provider by name
        ModelRunner runner = runnerProviders.stream()
            .filter(r -> r.metadata().name().equals(key.runnerName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unknown runner: " + key.runnerName()
            ));
        
        // Get tenant-specific configuration
        Map<String, Object> config = tenantConfig.getRunnerConfig(
            key.tenantId(), 
            key.runnerName()
        );
        
        // Initialize runner
        TenantContext ctx = TenantContext.of(key.tenantId());
        runner.initialize(manifest, config, ctx);
        
        // Warmup if configured
        if (config.getOrDefault("warmup.enabled", false).equals(true)) {
            runner.warmup(Collections.emptyList());
        }
        
        Log.infof("Created runner %s for model %s (tenant %s)", 
            key.runnerName(), key.modelId(), key.tenantId().value());
        
        return runner;
    }
    
    /**
     * Cleanup callback when runner is evicted
     */
    private void onRunnerEvicted(
        RunnerCacheKey key, 
        ModelRunner runner,
        RemovalCause cause
    ) {
        if (runner != null) {
            try {
                runner.close();
                Log.infof("Closed runner %s for model %s (cause: %s)", 
                    key.runnerName(), key.modelId(), cause);
            } catch (Exception e) {
                Log.errorf(e, "Error closing runner %s", key.runnerName());
            }
        }
        lastAccess.remove(key);
    }
    
    /**
     * Background cleanup of idle runners
     */
    private void startCleanupScheduler() {
        Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(
                this::cleanupIdleRunners,
                5, 5, TimeUnit.MINUTES
            );
    }
    
    private void cleanupIdleRunners() {
        Instant threshold = Instant.now().minus(IDLE_TIMEOUT);
        
        lastAccess.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(threshold)) {
                warmPool.invalidate(entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Cache key for runner pooling
     */
    private record RunnerCacheKey(
        TenantId tenantId,
        String modelId,
        String runnerName
    ) {}
}