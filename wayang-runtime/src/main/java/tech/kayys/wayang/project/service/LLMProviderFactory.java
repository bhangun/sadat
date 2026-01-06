package tech.kayys.wayang.project.service;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.project.dto.LLMConfig;

/**
 * LLM Provider Factory
 */
@ApplicationScoped
public class LLMProviderFactory {

    @Inject
    OpenAIProvider openAIProvider;

    @Inject
    AnthropicProvider anthropicProvider;

    public Uni<LLMProvider> createProvider(LLMConfig config) {
        return Uni.createFrom().item(() -> {
            return switch (config.provider.toLowerCase()) {
                case "openai", "azure-openai" -> (LLMProvider) openAIProvider;
                case "anthropic", "claude" -> (LLMProvider) anthropicProvider;
                default -> throw new IllegalArgumentException(
                        "Unsupported LLM provider: " + config.provider);
            };
        });
    }
}

/**
 * OpenAI Provider
 */
@ApplicationScoped
class OpenAIProvider implements LLMProvider {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(OpenAIProvider.class);

    @Override
    public Uni<String> complete(String prompt, LLMConfig config) {
        LOG.debug("Calling OpenAI with model: {}", config.model);
        return Uni.createFrom().item(() -> "Simulated OpenAI response for: " + prompt)
                .onItem().delayIt().by(java.time.Duration.ofMillis(500));
    }
}

/**
 * Anthropic Provider
 */
@ApplicationScoped
class AnthropicProvider implements LLMProvider {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(AnthropicProvider.class);

    @Override
    public Uni<String> complete(String prompt, LLMConfig config) {
        LOG.debug("Calling Anthropic with model: {}", config.model);
        return Uni.createFrom().item(() -> "Simulated Anthropic response")
                .onItem().delayIt().by(java.time.Duration.ofMillis(500));
    }
}
