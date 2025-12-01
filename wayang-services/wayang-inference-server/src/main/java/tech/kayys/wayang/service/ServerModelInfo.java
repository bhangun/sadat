package tech.kayys.wayang.service;

public class ServerModelInfo extends ModelInfo {
    private final Instant loadedAt;
    private final String serverId;
    private final Map<String, Object> serverMetrics;
    
    private ServerModelInfo(Builder builder) {
        super(builder.name, builder.description, builder.modelType, builder.architecture,
              builder.parameterCount, builder.quantization, builder.contextLength,
              builder.vocabSize, builder.fileSize, builder.metadata);
        this.loadedAt = builder.loadedAt;
        this.serverId = builder.serverId;
        this.serverMetrics = Collections.unmodifiableMap(new HashMap<>(builder.serverMetrics));
    }
    
    // Server-specific fields
    public Instant getLoadedAt() { return loadedAt; }
    public String getServerId() { return serverId; }
    public Map<String, Object> getServerMetrics() { return serverMetrics; }
    
    // Server-specific builder
    public static ServerBuilder builder() {
        return new ServerBuilder();
    }
    
    public static ServerBuilder fromCore(ModelInfo coreModelInfo) {
        return new ServerBuilder()
            .name(coreModelInfo.name())
            .description(coreModelInfo.description())
            .modelType(coreModelInfo.modelType())
            .architecture(coreModelInfo.architecture())
            .parameterCount(coreModelInfo.parameterCount())
            .quantization(coreModelInfo.quantization())
            .contextLength(coreModelInfo.contextLength())
            .vocabSize(coreModelInfo.vocabSize())
            .fileSize(coreModelInfo.fileSize())
            .metadata(coreModelInfo.metadata());
    }
    
    public static class ServerBuilder {
        private String name;
        private String description;
        private String modelType;
        private String architecture;
        private long parameterCount;
        private String quantization;
        private int contextLength;
        private int vocabSize;
        private long fileSize;
        private Map<String, Object> metadata = new HashMap<>();
        private Instant loadedAt = Instant.now();
        private String serverId;
        private Map<String, Object> serverMetrics = new HashMap<>();
        
        // Core builder methods
        public ServerBuilder name(String name) { this.name = name; return this; }
        public ServerBuilder description(String desc) { this.description = desc; return this; }
        public ServerBuilder modelType(String type) { this.modelType = type; return this; }
        public ServerBuilder architecture(String arch) { this.architecture = arch; return this; }
        public ServerBuilder parameterCount(long count) { this.parameterCount = count; return this; }
        public ServerBuilder quantization(String quant) { this.quantization = quant; return this; }
        public ServerBuilder contextLength(int len) { this.contextLength = len; return this; }
        public ServerBuilder vocabSize(int size) { this.vocabSize = size; return this; }
        public ServerBuilder fileSize(long size) { this.fileSize = size; return this; }
        public ServerBuilder metadata(Map<String, Object> meta) { this.metadata = meta; return this; }
        public ServerBuilder addMetadata(String key, Object value) { this.metadata.put(key, value); return this; }
        
        // Server-specific builder methods
        public ServerBuilder loadedAt(Instant loadedAt) { this.loadedAt = loadedAt; return this; }
        public ServerBuilder serverId(String serverId) { this.serverId = serverId; return this; }
        public ServerBuilder serverMetrics(Map<String, Object> metrics) { this.serverMetrics = metrics; return this; }
        public ServerBuilder addServerMetric(String key, Object value) { this.serverMetrics.put(key, value); return this; }
        
        public ServerModelInfo build() {
            return new ServerModelInfo(this);
        }
    }
}