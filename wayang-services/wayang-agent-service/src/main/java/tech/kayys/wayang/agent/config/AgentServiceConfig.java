package tech.kayys.wayang.agent.config;

import io.quarkus.arc.config.ConfigProperties;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;

@ConfigProperties(prefix = "wayang.agent")
@ApplicationScoped
public class AgentServiceConfig {
    
    public String defaultTenant = "default";
    public int defaultTimeoutMs = 30000;
    public boolean enableAuditLogging = true;
    public boolean enableMetrics = true;
    public Map<String, String> llmProviders;
    public List<String> allowedTools;
    public String defaultLlmModel = "gpt-4";
    public String defaultLlmProvider = "openai";
    public int maxConcurrentExecutions = 100;
    public int executionRetryAttempts = 3;
    public long executionRetryDelayMs = 1000;
}