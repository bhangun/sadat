package tech.kayys.wayang.standalone.core;

import lombok.Getter;
import tech.kayys.wayang.standalone.adapter.local.*;
import tech.kayys.wayang.standalone.adapter.remote.*;
import tech.kayys.wayang.standalone.cache.LocalCache;
import tech.kayys.wayang.standalone.security.SecretProvider;
import tech.kayys.wayang.standalone.state.InMemoryStateStore;
import tech.kayys.wayang.standalone.telemetry.LightTelemetryClient;
import tech.kayys.wayang.standalone.util.JsonUtil;

import java.io.Closeable;

/**
 * Runtime context holding all shared services and adapters.
 */
@Getter
public class RuntimeContext implements Closeable {
    
    private final RuntimeConfig config;
    private final JsonUtil jsonUtil;
    private final LocalCache cache;
    private final InMemoryStateStore stateStore;
    private final SecretProvider secretProvider;
    private final LightTelemetryClient telemetry;
    
    // Adapters
    private final LocalLLMAdapter localLLMAdapter;
    private final RemoteLLMAdapter remoteLLMAdapter;
    private final LocalRAGAdapter localRAGAdapter;
    private final RemoteRAGAdapter remoteRAGAdapter;
    private final LocalToolAdapter localToolAdapter;
    private final RemoteToolAdapter remoteToolAdapter;
    private final LocalMemoryAdapter localMemoryAdapter;
    private final RemoteMemoryAdapter remoteMemoryAdapter;
    
    public RuntimeContext(RuntimeConfig config) {
        this.config = config;
        this.jsonUtil = new JsonUtil();
        this.cache = new LocalCache(config.getCacheConfig());
        this.stateStore = new InMemoryStateStore();
        this.secretProvider = SecretProvider.create(config);
        this.telemetry = new LightTelemetryClient(config.getTelemetryConfig());
        
        // Initialize adapters based on config
        this.localLLMAdapter = config.isUseLocalLLM() ? new LocalLLMAdapter(this) : null;
        this.remoteLLMAdapter = config.isUseRemoteLLM() ? new RemoteLLMAdapter(this) : null;
        this.localRAGAdapter = config.isUseLocalRAG() ? new LocalRAGAdapter(this) : null;
        this.remoteRAGAdapter = config.isUseRemoteRAG() ? new RemoteRAGAdapter(this) : null;
        this.localToolAdapter = new LocalToolAdapter(this);
        this.remoteToolAdapter = new RemoteToolAdapter(this);
        this.localMemoryAdapter = new LocalMemoryAdapter(this);
        this.remoteMemoryAdapter = config.isUseRemoteMemory() ? new RemoteMemoryAdapter(this) : null;
    }
    
    public void initialize() {
        // Initialize components that need startup
        if (localLLMAdapter != null) {
            localLLMAdapter.initialize();
        }
        if (localRAGAdapter != null) {
            localRAGAdapter.initialize();
        }
        
        telemetry.start();
    }
    
    @Override
    public void close() {
        // Cleanup resources
        if (localLLMAdapter != null) {
            localLLMAdapter.close();
        }
        if (localRAGAdapter != null) {
            localRAGAdapter.close();
        }
        
        telemetry.stop();
        cache.clear();
        stateStore.clear();
    }
}