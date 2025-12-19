package tech.kayys.wayang.llm.provider;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import java.net.URI;

import jakarta.enterprise.context.ApplicationScoped;

import tech.kayys.wayang.agent.model.LLMConfig;
import tech.kayys.wayang.llm.model.LLMProvider;
import tech.kayys.wayang.workflow.model.ExecutionContext;

/**
 * Anthropic Claude Provider
 */
@ApplicationScoped
public class AnthropicProvider implements LLMProvider {

    private static final Logger LOG = Logger.getLogger(AnthropicProvider.class);
    private static final String DEFAULT_ENDPOINT = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @Override
    public Uni<String> complete(LLMConfig config, String prompt, ExecutionContext context) {
        String endpoint = config.getApiEndpoint() != null ? config.getApiEndpoint() : DEFAULT_ENDPOINT;

        JsonObject requestBody = new JsonObject()
                .put("model", config.getModel())
                .put("max_tokens", config.getParameters() != null && config.getParameters().getMaxTokens() != null
                        ? config.getParameters().getMaxTokens()
                        : 4096)
                .put("messages", new JsonArray()
                        .add(new JsonObject()
                                .put("role", "user")
                                .put("content", prompt)));

        // Add system prompt if provided
        if (config.getParameters() != null && config.getParameters().getSystemPrompt() != null) {
            requestBody.put("system", config.getParameters().getSystemPrompt());
        }

        // Add temperature if provided
        if (config.getParameters() != null && config.getParameters().getTemperature() != null) {
            requestBody.put("temperature", config.getParameters().getTemperature());
        }

        // Add top_p if provided
        if (config.getParameters() != null && config.getParameters().getTopP() != null) {
            requestBody.put("top_p", config.getParameters().getTopP());
        }

        LOG.debugf("Calling Anthropic API: %s with model: %s", endpoint, config.getModel());

        AnthropicClient client = RestClientBuilder.newBuilder()
                .baseUri(URI.create(endpoint))
                .build(AnthropicClient.class);

        return client.sendMessage(
                config.getApiKey(),
                ANTHROPIC_VERSION,
                requestBody)
                .map(this::extractResponseContent)
                .onFailure()
                .invoke(error -> LOG.errorf(error, "Anthropic API call failed for model: %s", config.getModel()));
    }

    private String extractResponseContent(JsonObject body) {
        JsonArray content = body.getJsonArray("content");

        if (content != null && content.size() > 0) {
            JsonObject firstContent = content.getJsonObject(0);
            return firstContent.getString("text");
        }

        throw new RuntimeException("No content in Anthropic response: " + body.encode());
    }

    @Override
    public LLMConfig.Provider getSupportedProvider() {
        return LLMConfig.Provider.ANTHROPIC;
    }
}
