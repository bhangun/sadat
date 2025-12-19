package tech.kayys.wayang.llm.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.model.LLMConfig;
import tech.kayys.wayang.llm.model.LLMProvider;
import tech.kayys.wayang.workflow.model.ExecutionContext;

import org.jboss.logging.Logger;

/**
 * LLM Service - handles interactions with LLM providers
 */
@ApplicationScoped
public class LLMService {

    private static final Logger LOG = Logger.getLogger(LLMService.class);

    @Inject
    LLMProviderRegistry providerRegistry;

    /**
     * Complete text using LLM
     */
    public Uni<String> complete(LLMConfig config, String prompt, ExecutionContext context) {
        LOG.debugf("LLM completion with provider: %s, model: %s",
                config.getProvider(), config.getModel());

        LLMProvider provider = providerRegistry.getProvider(config.getProvider());

        return provider.complete(config, prompt, context)
                .onFailure().retry().atMost(
                        config.getRetryConfig() != null ? config.getRetryConfig().getMaxRetries() : 3)
                .onFailure().recoverWithUni(error -> {
                    LOG.errorf(error, "LLM completion failed, trying fallback models");
                    return tryFallbackModels(config, prompt, context, provider);
                });
    }

    /**
     * Try fallback models on failure
     */
    private Uni<String> tryFallbackModels(LLMConfig config, String prompt,
            ExecutionContext context, LLMProvider provider) {
        if (config.getFallbackModels() == null || config.getFallbackModels().isEmpty()) {
            return Uni.createFrom().failure(
                    new RuntimeException("LLM completion failed and no fallback models configured"));
        }

        // Try first fallback model
        String fallbackModel = config.getFallbackModels().get(0);
        LLMConfig fallbackConfig = copyConfigWithModel(config, fallbackModel);

        return provider.complete(fallbackConfig, prompt, context);
    }

    private LLMConfig copyConfigWithModel(LLMConfig original, String model) {
        LLMConfig copy = new LLMConfig();
        copy.setProvider(original.getProvider());
        copy.setModel(model);
        copy.setApiKey(original.getApiKey());
        copy.setApiEndpoint(original.getApiEndpoint());
        copy.setParameters(original.getParameters());
        copy.setRetryConfig(original.getRetryConfig());
        return copy;
    }
}
