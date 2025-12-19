package tech.kayys.wayang.agent.service;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class LLMProviderRegistry {

    public Uni<Void> registerProvider(String providerId, Map<String, Object> config) {
        Log.infof("Registering LLM provider: %s", providerId);
        // In a real implementation, this would register the provider
        return Uni.createFrom().voidItem();
    }

    public Uni<Void> updateProvider(String providerId, Map<String, Object> config) {
        Log.infof("Updating LLM provider: %s", providerId);
        // In a real implementation, this would update the provider configuration
        return Uni.createFrom().voidItem();
    }

    public Uni<Map<String, Object>> getProviderConfig(String providerId) {
        Log.infof("Getting config for LLM provider: %s", providerId);
        // Return mock configuration
        return Uni.createFrom().item(Map.of("provider", providerId, "status", "active"));
    }
}