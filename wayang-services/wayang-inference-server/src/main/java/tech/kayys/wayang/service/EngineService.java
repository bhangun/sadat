package tech.kayys.wayang.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import tech.kayys.wayang.engine.LlamaConfig;
import tech.kayys.wayang.engine.LlamaConfig.SamplingConfig;
import tech.kayys.wayang.engine.LlamaEngine;
import tech.kayys.wayang.mcp.Metrics;
import tech.kayys.wayang.model.ChatMessage;
import tech.kayys.wayang.model.EmbeddingResult;
import tech.kayys.wayang.model.GenerationResult;
import tech.kayys.wayang.model.ModelInfo;
import tech.kayys.wayang.plugin.FunctionRegistry;
import tech.kayys.wayang.plugin.ServerConfig;
import tech.kayys.wayang.utils.ModelDownloader;

public class EngineService {
    private static final Logger log = Logger.getLogger(EngineService.class);
    
    @Inject
    ServerConfig serverConfig;
    
    @Inject
    FunctionRegistry functionRegistry;
    
    @Inject
    Metrics metrics;
    
    @Inject
    Event<ServiceEvent> serviceEvent;
    
    private LlamaEngine engine;
    private HealthCheck healthCheck;
    private ScheduledExecutorService healthCheckScheduler;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean healthy = new AtomicBoolean(false);
    
    @PostConstruct
    void initialize() {
        if (!initialized.compareAndSet(false, true)) {
            log.warn("EngineService already initialized");
            return;
        }
        
        log.info("Initializing Llama Engine Service...");
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate configuration first
            validateServerConfig();
            
            String modelPath = resolveModelPath();
            
            LlamaConfig config = buildEngineConfig(modelPath);
            
            // Initialize engine with plugins if configured
            engine = initializeEngine(config);
            
            // Initialize health checking
            initializeHealthCheck();
            
            // Register metrics
            registerMetrics();
            
            long initializationTime = System.currentTimeMillis() - startTime;
            log.info("Llama Engine Service initialized successfully in {} ms", initializationTime);
            metrics.recordHistogram("service.initialization_time_ms", initializationTime);
            
            healthy.set(true);
            serviceEvent.fire(new ServiceEvent(ServiceEvent.Type.STARTED, "Engine service started successfully"));
            
        } catch (Exception e) {
            log.error("Failed to initialize engine service", e);
            metrics.incrementCounter("service.initialization_failures");
            serviceEvent.fire(new ServiceEvent(ServiceEvent.Type.ERROR, "Engine service failed to start: " + e.getMessage()));
            throw new RuntimeException("Failed to initialize engine service", e);
        }
    }
    
    @PreDestroy
    void cleanup() {
        log.info("Shutting down Engine Service...");
        
        if (healthCheckScheduler != null) {
            healthCheckScheduler.shutdown();
            try {
                if (!healthCheckScheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                    healthCheckScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                healthCheckScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (engine != null) {
            try {
                engine.close();
                log.info("Engine closed successfully");
            } catch (Exception e) {
                log.error("Error during engine shutdown", e);
            }
        }
        
        initialized.set(false);
        healthy.set(false);
        serviceEvent.fire(new ServiceEvent(ServiceEvent.Type.STOPPED, "Engine service stopped"));
        log.info("Engine Service shutdown completed");
    }
    
    private void validateServerConfig() {
        if (serverConfig == null) {
            throw new IllegalStateException("ServerConfig is not injected");
        }
        
        if (serverConfig.libraryPath() == null || serverConfig.libraryPath().trim().isEmpty()) {
            throw new IllegalArgumentException("Library path cannot be null or empty");
        }
        
        if (!Files.exists(Path.of(serverConfig.libraryPath()))) {
            throw new IllegalArgumentException("Library file not found: " + serverConfig.libraryPath());
        }
        
        // If auto-download is disabled, validate model path exists
        if (!serverConfig.autoDownload().enabled()) {
            if (serverConfig.modelPath() == null || serverConfig.modelPath().trim().isEmpty()) {
                throw new IllegalArgumentException("Model path cannot be null or empty when auto-download is disabled");
            }
            if (!Files.exists(Path.of(serverConfig.modelPath()))) {
                throw new IllegalArgumentException("Model file not found: " + serverConfig.modelPath());
            }
        }
    }
    
    private String resolveModelPath() throws IOException {
        if (serverConfig.autoDownload().enabled()) {
            return downloadModel().toString();
        } else {
            // Verify model exists
            Path modelPath = Path.of(serverConfig.modelPath());
            if (!Files.exists(modelPath)) {
                throw new IOException("Model file not found: " + modelPath);
            }
            return serverConfig.modelPath();
        }
    }
    
    private LlamaConfig buildEngineConfig(String modelPath) {
        return LlamaConfig.builder()
            .libraryPath(serverConfig.libraryPath())
            .modelPath(modelPath)
            .contextSize(serverConfig.contextSize())
            .batchSize(serverConfig.batchSize())
            .uBatchSize(serverConfig.batchSize()) // Use same as batch size if not specified
            .threads(serverConfig.threads())
            .threadsBatch(serverConfig.threads()) // Use same as threads if not specified
            .gpuLayers(serverConfig.gpuLayers())
            .useMmap(serverConfig.useMmap())
            .useMlock(serverConfig.useMlock())
            .ropeFreqBase(serverConfig.ropeFreqBase())
            .ropeFreqScale(serverConfig.ropeFreqScale())
            .seed(serverConfig.seed())
            .embeddings(serverConfig.embeddings())
            .flashAttention(serverConfig.flashAttention())
            .warmupModel(serverConfig.warmupModel())
            .warmupTokens(serverConfig.warmupTokens())
            .enablePromptCache(serverConfig.enablePromptCache())
            .promptCacheSize(serverConfig.promptCacheSize())
            .maxTokensLimit(serverConfig.maxTokensLimit())
            .validateInput(serverConfig.validateInput())
            .sampling(serverConfig.sampling())
            .healthCheck(serverConfig.healthCheck())
            .build();
    }
    
    private LlamaEngine initializeEngine(LlamaConfig config) {
        Path pluginsDir = serverConfig.pluginsDirectory() != null ? 
            Path.of(serverConfig.pluginsDirectory()) : null;
        Path dataDir = serverConfig.dataDirectory() != null ? 
            Path.of(serverConfig.dataDirectory()) : null;
            
        return new LlamaEngine(config, pluginsDir, dataDir);
    }
    
    private void initializeHealthCheck() {
        if (serverConfig.healthCheck().enabled()) {
            healthCheck = new EngineHealthCheck(engine, serverConfig.healthCheck());
            healthCheckScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "engine-health-check");
                t.setDaemon(true);
                return t;
            });
            
            healthCheckScheduler.scheduleAtFixedRate(
                this::performHealthCheck,
                serverConfig.healthCheck().intervalMs(),
                serverConfig.healthCheck().intervalMs(),
                TimeUnit.MILLISECONDS
            );
            log.info("Health check scheduler started with interval: {} ms", 
                serverConfig.healthCheck().intervalMs());
        }
    }
    
    private void performHealthCheck() {
        try {
            boolean isHealthy = healthCheck.check();
            if (healthy.compareAndSet(!isHealthy, isHealthy)) {
                if (isHealthy) {
                    log.info("Engine health check passed");
                    serviceEvent.fire(new ServiceEvent(ServiceEvent.Type.HEALTHY, "Engine is healthy"));
                } else {
                    log.warn("Engine health check failed");
                    serviceEvent.fire(new ServiceEvent(ServiceEvent.Type.UNHEALTHY, "Engine is unhealthy"));
                }
            }
            metrics.setGauge("service.healthy", isHealthy ? 1 : 0);
        } catch (Exception e) {
            log.error("Health check execution failed", e);
            healthy.set(false);
            metrics.setGauge("service.healthy", 0);
        }
    }
    
    private void registerMetrics() {
        // Register engine metrics with service metrics
        if (engine.getMetrics() != null) {
            // You might want to periodically sync engine metrics with service metrics
            // or expose engine metrics through the service metrics registry
        }
    }
    
    private Path downloadModel() throws IOException {
        var autoDownloadConfig = serverConfig.autoDownload();
        Path downloadDir = Paths.get(autoDownloadConfig.downloadDir());
        
        // Create download directory if it doesn't exist
        Files.createDirectories(downloadDir);
        
        ModelDownloader downloader = new ModelDownloader(downloadDir);
        
        log.info("Auto-downloading model from HuggingFace: {}/{} to {}",
            autoDownloadConfig.repoId(), autoDownloadConfig.filename(), downloadDir);
        
        try {
            Path modelPath = downloader.downloadFromHuggingFace(
                autoDownloadConfig.repoId(),
                autoDownloadConfig.filename(),
                progress -> {
                    if (progress.percentage() % 10 == 0) { // Log every 10%
                        log.infof("Download progress: %.1f%% (%d/%d MB)",
                            progress.percentage() * 100,
                            progress.downloaded() / (1024 * 1024),
                            progress.total() / (1024 * 1024));
                    }
                }
            );
            
            log.info("Model downloaded successfully to: {}", modelPath);
            metrics.incrementCounter("model.downloads.completed");
            return modelPath;
            
        } catch (Exception e) {
            metrics.incrementCounter("model.downloads.failed");
            throw new IOException("Failed to download model from HuggingFace", e);
        }
    }
    
    // Public API methods with enhanced error handling and monitoring
    public GenerationResult generate(String prompt, SamplingConfig config, 
                                    int maxTokens, Consumer<String> callback) {
        ensureInitialized();
        ensureHealthy();
        
        long startTime = System.currentTimeMillis();
        metrics.incrementCounter("requests.generate");
        
        try {
            GenerationResult result = engine.generate(prompt, config, maxTokens, null, callback);
            metrics.recordHistogram("requests.generate.time_ms", System.currentTimeMillis() - startTime);
            metrics.incrementCounter("requests.generate.success");
            return result;
        } catch (Exception e) {
            metrics.incrementCounter("requests.generate.failed");
            log.error("Generation request failed", e);
            throw e;
        }
    }
    
    public GenerationResult chat(List<ChatMessage> messages, SamplingConfig config,
                                int maxTokens, Consumer<String> callback) {
        ensureInitialized();
        ensureHealthy();
        
        long startTime = System.currentTimeMillis();
        metrics.incrementCounter("requests.chat");
        
        try {
            GenerationResult result = engine.chat(messages, config, maxTokens, callback);
            metrics.recordHistogram("requests.chat.time_ms", System.currentTimeMillis() - startTime);
            metrics.incrementCounter("requests.chat.success");
            return result;
        } catch (Exception e) {
            metrics.incrementCounter("requests.chat.failed");
            log.error("Chat request failed", e);
            throw e;
        }
    }
    
    public EmbeddingResult embeddings(List<String> texts) {
        ensureInitialized();
        ensureHealthy();
        
        long startTime = System.currentTimeMillis();
        metrics.incrementCounter("requests.embeddings");
        
        try {
            EmbeddingResult result = engine.embeddings(texts);
            metrics.recordHistogram("requests.embeddings.time_ms", System.currentTimeMillis() - startTime);
            metrics.incrementCounter("requests.embeddings.success");
            return result;
        } catch (Exception e) {
            metrics.incrementCounter("requests.embeddings.failed");
            log.error("Embeddings request failed", e);
            throw e;
        }
    }
    
    public ModelInfo getModelInfo() {
        ensureInitialized();
        return engine.getModelInfo();
    }
    
    public HealthStatus getHealthStatus() {
        if (!initialized.get()) {
            return HealthStatus.STARTING;
        }
        return healthy.get() ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
    }
    
    public Metrics getMetrics() {
        return metrics;
    }
    
    private void ensureInitialized() {
        if (!initialized.get() || engine == null) {
            throw new IllegalStateException("EngineService is not initialized");
        }
    }
    
    private void ensureHealthy() {
        if (!healthy.get()) {
            throw new IllegalStateException("EngineService is not healthy");
        }
    }
    
    // Health check implementation
    private static class EngineHealthCheck {
        private final LlamaEngine engine;
        private final LlamaConfig.HealthCheckConfig config;
        
        EngineHealthCheck(LlamaEngine engine, LlamaConfig.HealthCheckConfig config) {
            this.engine = engine;
            this.config = config;
        }
        
        boolean check() {
            try {
                // Simple health check: generate a small completion
                String testPrompt = "Hello";
                engine.generate(testPrompt, 
                    new LlamaConfig.DefaultSamplingConfig(),
                    config.warmupGenerationTokens(),
                    List.of(),
                    null,
                    false);
                return true;
            } catch (Exception e) {
                log.debug("Health check failed", e);
                return false;
            }
        }
    }
    
    // Event types
    public static class ServiceEvent {
        public enum Type { STARTED, STOPPED, HEALTHY, UNHEALTHY, ERROR }
        
        private final Type type;
        private final String message;
        private final long timestamp;
        
        public ServiceEvent(Type type, String message) {
            this.type = type;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters
        public Type getType() { return type; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
    
    public enum HealthStatus {
        STARTING, HEALTHY, UNHEALTHY
    }
}