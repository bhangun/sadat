package tech.kayys.chat;

import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tech.kayys.wayang.engine.LlamaConfig;
import tech.kayys.wayang.engine.LlamaEngine;
import tech.kayys.wayang.engine.LlamaConfig.SamplingConfig;
import tech.kayys.wayang.model.ChatMessage;
import tech.kayys.wayang.utils.ModelDownloader;

public class ChatbotController {
    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);
    
    private LlamaEngine engine;
    private SamplingConfig defaultSampling;
    
    public ChatbotController() {
        this.defaultSampling = SamplingConfig.defaults();
    }
    
    public void initialize(LlamaConfig config) {
        if (engine != null) {
            engine.close();
        }
        
        log.info("Initializing Llama engine...");
        engine = new LlamaEngine(config);
        log.info("Engine initialized successfully");
    }
    
    public void chat(List<ChatMessage> messages, Consumer<String> streamCallback) {
        if (engine == null) {
            throw new IllegalStateException("Engine not initialized");
        }
        
        engine.chat(messages, defaultSampling, 512, streamCallback);
    }
    
    public void downloadModel(String repoId, String filename, String downloadDir,
                             Consumer<ModelDownloader.DownloadProgress> progressCallback) {
        new Thread(() -> {
            try {
                ModelDownloader downloader = new ModelDownloader(Paths.get(downloadDir));
                downloader.downloadFromHuggingFace(repoId, filename, progressCallback);
            } catch (Exception e) {
                log.error("Download failed", e);
            }
        }).start();
    }
    
    public boolean isInitialized() {
        return engine != null;
    }
    
    public String getModelInfo() {
        return engine != null ? engine.getModelDesc() : "Not loaded";
    }
    
    public void setSamplingConfig(SamplingConfig config) {
        this.defaultSampling = config;
    }
}
