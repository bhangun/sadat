package tech.kayys.wayang.plugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import tech.kayys.wayang.engine.LlamaConfig;
import tech.kayys.wayang.engine.LlamaEngine;
import tech.kayys.wayang.model.ModelInfo;

public class ModelManager {
    private static final Logger log = Logger.getLogger(ModelManager.class);
    
    @Inject
    ServerConfig serverConfig;
    
    @Inject
    FunctionRegistry functionRegistry;
    
    private final Map<String, LlamaEngine> loadedModels = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private String activeModelId = "default";
    
    @PostConstruct
    void initialize() {
        log.info("Initializing Model Manager...");
        
        try {
            // Load default model
            loadDefaultModel();
            log.info("Model Manager initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize Model Manager", e);
            throw new RuntimeException(e);
        }
    }
    
    private void loadDefaultModel() {
        String modelPath = serverConfig.modelPath();
        
        if (modelPath == null || modelPath.isBlank()) {
            if (serverConfig.autoDownload().enabled()) {
                modelPath = downloadModel();
            } else {
                throw new IllegalStateException("No model path configured");
            }
        }
        
        LlamaConfig config = buildConfig(modelPath);
        LlamaEngine engine = new LlamaEngine(config, functionRegistry);
        
        loadedModels.put(activeModelId, engine);
        log.infof("Default model loaded: {}", engine.getModelInfo().name());
    }
    
    private String downloadModel() {
        // Implementation uses ModelDownloader from core
        log.info("Auto-download not implemented yet");
        throw new UnsupportedOperationException("Auto-download coming soon");
    }
    
    private LlamaConfig buildConfig(String modelPath) {
        return LlamaConfig.builder()
            .libraryPath(serverConfig.libraryPath())
            .modelPath(modelPath)
            .contextSize(serverConfig.contextSize())
            .batchSize(serverConfig.batchSize())
            .threads(serverConfig.threads())
            .gpuLayers(serverConfig.gpuLayers())
            .ropeFreqBase(serverConfig.ropeFreqBase())
            .ropeFreqScale(serverConfig.ropeFreqScale())
            .seed(serverConfig.seed())
            .useMmap(serverConfig.useMmap())
            .useMlock(serverConfig.useMlock())
            .embeddings(serverConfig.embeddings())
            .flashAttention(serverConfig.flashAttention())
            .build();
    }
    
    public LlamaEngine getActiveModel() {
        lock.readLock().lock();
        try {
            LlamaEngine engine = loadedModels.get(activeModelId);
            if (engine == null) {
                throw new IllegalStateException("No active model loaded");
            }
            return engine;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public Map<String, ModelInfo> listModels() {
        Map<String, ModelInfo> models = new ConcurrentHashMap<>();
        loadedModels.forEach((id, engine) -> 
            models.put(id, engine.getModelInfo()));
        return models;
    }
    
    public void loadModel(String modelId, String modelPath) {
        lock.writeLock().lock();
        try {
            if (loadedModels.containsKey(modelId)) {
                throw new IllegalArgumentException("Model already loaded: " + modelId);
            }
            
            if (loadedModels.size() >= serverConfig.modelManager().maxLoadedModels()) {
                throw new IllegalStateException("Maximum number of models loaded");
            }
            
            LlamaConfig config = buildConfig(modelPath);
            LlamaEngine engine = new LlamaEngine(config, functionRegistry);
            
            loadedModels.put(modelId, engine);
            log.info("Model loaded: {} -> {}", modelId, engine.getModelInfo().name());
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void unloadModel(String modelId) {
        lock.writeLock().lock();
        try {
            if (modelId.equals(activeModelId)) {
                throw new IllegalArgumentException("Cannot unload active model");
            }
            
            LlamaEngine engine = loadedModels.remove(modelId);
            if (engine != null) {
                engine.close();
                log.info("Model unloaded: {}", modelId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void switchModel(String modelId) {
        lock.writeLock().lock();
        try {
            if (!loadedModels.containsKey(modelId)) {
                throw new IllegalArgumentException("Model not loaded: " + modelId);
            }
            
            activeModelId = modelId;
            log.info("Switched to model: {}", modelId);
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @PreDestroy
    void cleanup() {
        log.info("Shutting down Model Manager...");
        
        loadedModels.values().forEach(engine -> {
            try {
                engine.close();
            } catch (Exception e) {
                log.error("Error closing engine", e);
            }
        });
        
        loadedModels.clear();
    }
}
