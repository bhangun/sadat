package tech.kayys.wayang.plugin;

import io.smallrye.config.WithDefault;

public interface ServerConfig {
    String libraryPath();
    String modelPath();
    
    @WithDefault("4096")
    int contextSize();
    
    @WithDefault("512")
    int batchSize();
    
    @WithDefault("8")
    int threads();
    
    @WithDefault("0")
    int gpuLayers();
    
    @WithDefault("10000.0")
    float ropeFreqBase();
    
    @WithDefault("1.0")
    float ropeFreqScale();
    
    @WithDefault("-1")
    int seed();
    
    AutoDownloadConfig autoDownload();
    
    interface AutoDownloadConfig {
        @WithDefault("false")
        boolean enabled();
        
        String repoId();
        String filename();
        
        @WithDefault("./models")
        String downloadDir();
    }
}
