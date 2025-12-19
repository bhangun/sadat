package tech.kayys.wayang.llm.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import tech.kayys.wayang.agent.model.LLMConfig;
import tech.kayys.wayang.llm.model.LLMProvider;

/**
 * LLM Provider Registry
 */
@ApplicationScoped
public class LLMProviderRegistry {

    private final Map<LLMConfig.Provider, LLMProvider> providers = new HashMap<>();

    @Inject
    public LLMProviderRegistry(Instance<LLMProvider> providerInstances) {
        providerInstances.forEach(provider -> providers.put(provider.getSupportedProvider(), provider));
    }

    public LLMProvider getProvider(LLMConfig.Provider providerType) {
        LLMProvider provider = providers.get(providerType);
        if (provider == null) {
            throw new IllegalArgumentException("No provider found for: " + providerType);
        }
        return provider;
    }
}
