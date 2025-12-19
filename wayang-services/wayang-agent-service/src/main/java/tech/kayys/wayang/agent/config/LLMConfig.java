package tech.kayys.wayang.agent.config;

import io.quarkus.arc.config.ConfigProperties;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ConfigProperties(prefix = "wayang.llm")
@ApplicationScoped
public class LLMConfig {
    
    public String defaultProvider = "openai";
    public String defaultModel = "gpt-4";
    public int defaultTimeoutMs = 30000;
    public int maxTokens = 4096;
    public double temperature = 0.7;
    public Map<String, ProviderConfig> providers;
    
    public static class ProviderConfig {
        public String apiKey;
        public String baseUrl;
        public String model;
        public int timeoutMs;
        public int maxRetries;
    }
}