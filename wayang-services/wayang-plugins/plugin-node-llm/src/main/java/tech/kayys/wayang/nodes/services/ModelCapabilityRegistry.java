
/**
 * Model capability registry
 */
@ApplicationScoped
public class ModelCapabilityRegistry {
    
    private final Map<String, ModelInfo> models = new ConcurrentHashMap<>();
    
    @PostConstruct
    void init() {
        // Register default models
        registerModel(ModelInfo.builder()
            .id("gpt-4")
            .maxTokens(8192)
            .supportsFunction(true)
            .available(true)
            .build());
        
        registerModel(ModelInfo.builder()
            .id("claude-3-sonnet")
            .maxTokens(200000)
            .supportsFunction(false)
            .available(true)
            .build());
    }
    
    public void registerModel(ModelInfo model) {
        models.put(model.getId(), model);
    }
    
    public ModelInfo getModel(String modelId) {
        return models.get(modelId);
    }
    
    public ModelInfo getDefaultModel() {
        return models.values().stream()
            .filter(ModelInfo::isAvailable)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No available models"));
    }
    
    public ModelInfo getDefaultEmbeddingModel() {
        return ModelInfo.builder()
            .id("text-embedding-3-small")
            .maxTokens(8191)
            .available(true)
            .build();
    }
    
    public ModelInfo findBestModel(List<String> capabilities) {
        return models.values().stream()
            .filter(ModelInfo::isAvailable)
            .filter(m -> hasCapabilities(m, capabilities))
            .findFirst()
            .orElse(getDefaultModel());
    }
    
    private boolean hasCapabilities(ModelInfo model, List<String> capabilities) {
        for (var cap : capabilities) {
            if ("function_calling".equals(cap) && !model.supportsFunction()) {
                return false;
            }
        }
        return true;
    }
}