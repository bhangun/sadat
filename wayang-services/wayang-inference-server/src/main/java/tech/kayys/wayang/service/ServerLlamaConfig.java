package tech.kayys.wayang.service;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import tech.kayys.wayang.engine.LlamaConfig;

public interface ServerLlamaConfig extends LlamaConfig {
    // === SERVER-SPECIFIC CONFIGURATION ===
    int serverPort();
    String serverHost();
    int requestTimeoutMs();
    int maxConcurrentRequests();
    int maxRequestSize();
    boolean warmupModel();
    int warmupTokens();
    boolean enablePromptCache();
    int promptCacheSize();
    String logLevel();
    boolean enableMetrics();
    boolean enableTracing();
    int maxTokensLimit();
    int maxStopSequences();
    boolean validateInput();
    String pluginsDirectory();
    String dataDirectory();
    List<String> enabledPlugins();
    HealthCheckConfig healthCheck();
    int shutdownTimeoutMs();
    
    // === SERVER-SPECIFIC HEALTH CHECK ===
    interface HealthCheckConfig {
        boolean enabled();
        int intervalMs();
        int warmupPromptTokens();
        int warmupGenerationTokens();
        int timeoutMs();
    }
    
    // === SERVER BUILDER ===
    static ServerBuilder builder() {
        return new ServerBuilder();
    }
    
    class ServerBuilder extends CoreBuilder {
        private int serverPort = 8080;
        private String serverHost = "0.0.0.0";
        private int requestTimeoutMs = 60000;
        private int maxConcurrentRequests = 100;
        private int maxRequestSize = 104857600;
        private boolean warmupModel = true;
        private int warmupTokens = 256;
        private boolean enablePromptCache = true;
        private int promptCacheSize = 1000;
        private String logLevel = "INFO";
        private boolean enableMetrics = true;
        private boolean enableTracing = false;
        private int maxTokensLimit = 8192;
        private int maxStopSequences = 10;
        private boolean validateInput = true;
        private String pluginsDirectory = "";
        private String dataDirectory = "";
        private List<String> enabledPlugins = List.of();
        private HealthCheckConfig healthCheck = new DefaultHealthCheckConfig();
        private int shutdownTimeoutMs = 30000;
        
        // Server-specific builder methods
        public ServerBuilder serverPort(int port) { this.serverPort = port; return this; }
        public ServerBuilder serverHost(String host) { this.serverHost = host; return this; }
        public ServerBuilder requestTimeoutMs(int timeout) { this.requestTimeoutMs = timeout; return this; }
        public ServerBuilder maxConcurrentRequests(int max) { this.maxConcurrentRequests = max; return this; }
        public ServerBuilder maxRequestSize(int size) { this.maxRequestSize = size; return this; }
        public ServerBuilder warmupModel(boolean warmup) { this.warmupModel = warmup; return this; }
        public ServerBuilder warmupTokens(int tokens) { this.warmupTokens = tokens; return this; }
        public ServerBuilder enablePromptCache(boolean enable) { this.enablePromptCache = enable; return this; }
        public ServerBuilder promptCacheSize(int size) { this.promptCacheSize = size; return this; }
        public ServerBuilder logLevel(String level) { this.logLevel = level; return this; }
        public ServerBuilder enableMetrics(boolean enable) { this.enableMetrics = enable; return this; }
        public ServerBuilder enableTracing(boolean enable) { this.enableTracing = enable; return this; }
        public ServerBuilder maxTokensLimit(int limit) { this.maxTokensLimit = limit; return this; }
        public ServerBuilder maxStopSequences(int max) { this.maxStopSequences = max; return this; }
        public ServerBuilder validateInput(boolean validate) { this.validateInput = validate; return this; }
        public ServerBuilder pluginsDirectory(String dir) { this.pluginsDirectory = dir; return this; }
        public ServerBuilder dataDirectory(String dir) { this.dataDirectory = dir; return this; }
        public ServerBuilder enabledPlugins(List<String> plugins) { this.enabledPlugins = plugins; return this; }
        public ServerBuilder healthCheck(HealthCheckConfig config) { this.healthCheck = config; return this; }
        public ServerBuilder shutdownTimeoutMs(int timeout) { this.shutdownTimeoutMs = timeout; return this; }
        
        @Override
        public ServerLlamaConfig build() {
            CoreLlamaConfig coreConfig = super.build();
            return new ServerLlamaConfigImpl(coreConfig, serverPort, serverHost, requestTimeoutMs,
                maxConcurrentRequests, maxRequestSize, warmupModel, warmupTokens, enablePromptCache,
                promptCacheSize, logLevel, enableMetrics, enableTracing, maxTokensLimit,
                maxStopSequences, validateInput, pluginsDirectory, dataDirectory, enabledPlugins,
                healthCheck, shutdownTimeoutMs);
        }
    }
    
    // Server implementation that delegates to core
    record ServerLlamaConfigImpl(
        CoreLlamaConfig coreConfig,
        int serverPort,
        String serverHost,
        int requestTimeoutMs,
        int maxConcurrentRequests,
        int maxRequestSize,
        boolean warmupModel,
        int warmupTokens,
        boolean enablePromptCache,
        int promptCacheSize,
        String logLevel,
        boolean enableMetrics,
        boolean enableTracing,
        int maxTokensLimit,
        int maxStopSequences,
        boolean validateInput,
        String pluginsDirectory,
        String dataDirectory,
        List<String> enabledPlugins,
        HealthCheckConfig healthCheck,
        int shutdownTimeoutMs
    ) implements ServerLlamaConfig {
        
        // Delegate all core methods to coreConfig
        @Override public String libraryPath() { return coreConfig.libraryPath(); }
        @Override public String modelPath() { return coreConfig.modelPath(); }
        @Override public int gpuLayers() { return coreConfig.gpuLayers(); }
        @Override public boolean useMmap() { return coreConfig.useMmap(); }
        @Override public boolean useMlock() { return coreConfig.useMlock(); }
        @Override public boolean vocabOnly() { return coreConfig.vocabOnly(); }
        @Override public int contextSize() { return coreConfig.contextSize(); }
        @Override public int batchSize() { return coreConfig.batchSize(); }
        @Override public int uBatchSize() { return coreConfig.uBatchSize(); }
        @Override public int threads() { return coreConfig.threads(); }
        @Override public int threadsBatch() { return coreConfig.threadsBatch(); }
        @Override public float ropeFreqBase() { return coreConfig.ropeFreqBase(); }
        @Override public float ropeFreqScale() { return coreConfig.ropeFreqScale(); }
        @Override public int ropeScalingType() { return coreConfig.ropeScalingType(); }
        @Override public int nSeqMax() { return coreConfig.nSeqMax(); }
        @Override public int poolingType() { return coreConfig.poolingType(); }
        @Override public int attentionType() { return coreConfig.attentionType(); }
        @Override public boolean embeddings() { return coreConfig.embeddings(); }
        @Override public boolean flashAttention() { return coreConfig.flashAttention(); }
        @Override public boolean offloadKqv() { return coreConfig.offloadKqv(); }
        @Override public float defragThreshold() { return coreConfig.defragThreshold(); }
        @Override public float yarnExtFactor() { return coreConfig.yarnExtFactor(); }
        @Override public float yarnAttnFactor() { return coreConfig.yarnAttnFactor(); }
        @Override public float yarnBetaFast() { return coreConfig.yarnBetaFast(); }
        @Override public float yarnBetaSlow() { return coreConfig.yarnBetaSlow(); }
        @Override public int yarnOrigCtx() { return coreConfig.yarnOrigCtx(); }
        @Override public int seed() { return coreConfig.seed(); }
        @Override public String tensorSplit() { return coreConfig.tensorSplit(); }
        @Override public int mainGpu() { return coreConfig.mainGpu(); }
        @Override public int splitMode() { return coreConfig.splitMode(); }
        @Override public SamplingConfig sampling() { return coreConfig.sampling(); }
    }
    
    class DefaultHealthCheckConfig implements HealthCheckConfig {
        @Override public boolean enabled() { return true; }
        @Override public int intervalMs() { return 30000; }
        @Override public int warmupPromptTokens() { return 1000; }
        @Override public int warmupGenerationTokens() { return 10; }
        @Override public int timeoutMs() { return 5000; }
    }
}