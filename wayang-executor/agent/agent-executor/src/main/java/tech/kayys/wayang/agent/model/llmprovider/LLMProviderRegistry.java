package tech.kayys.wayang.agent.model.llmprovider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.LLMProvider;

@ApplicationScoped
public class LLMProviderRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(LLMProviderRegistry.class);

    private final Map<String, LLMProvider> providers = new ConcurrentHashMap<>();

    @jakarta.inject.Inject
    OpenAIProvider openAIProvider;

    @jakarta.inject.Inject
    AnthropicProvider anthropicProvider;

    @jakarta.annotation.PostConstruct
    void init() {
        registerProvider(openAIProvider);
        registerProvider(anthropicProvider);

        LOG.info("Registered {} LLM providers", providers.size());
    }

    public void registerProvider(LLMProvider provider) {
        providers.put(provider.name(), provider);
        LOG.info("Registered LLM provider: {}", provider.name());
    }

    public Uni<LLMProvider> getProvider(String name) {
        LLMProvider provider = providers.get(name.toLowerCase());

        if (provider == null) {
            LOG.error("Provider not found: {}", name);
            return Uni.createFrom().failure(
                    new IllegalArgumentException("Provider not found: " + name));
        }

        return Uni.createFrom().item(provider);
    }

    public List<String> getAvailableProviders() {
        return new ArrayList<>(providers.keySet());
    }

    public Map<String, List<String>> getAllSupportedModels() {
        Map<String, List<String>> result = new HashMap<>();

        providers.forEach((name, provider) -> result.put(name, provider.supportedModels()));

        return result;
    }
}
