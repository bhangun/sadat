package tech.kayys.wayang.llm.provider;

import org.jboss.logging.Logger;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import java.net.URI;
import tech.kayys.wayang.agent.model.LLMConfig;
import tech.kayys.wayang.llm.model.LLMProvider;
import tech.kayys.wayang.workflow.model.ExecutionContext;

/**
 * Azure OpenAI Provider
 */
@ApplicationScoped
public class AzureOpenAIProvider implements LLMProvider {

    private static final Logger LOG = Logger.getLogger(AzureOpenAIProvider.class);

    @Override
    public Uni<String> complete(LLMConfig config, String prompt, ExecutionContext context) {
        // Azure OpenAI requires specific endpoint format:
        // https://{resource-name}.openai.azure.com/openai/deployments/{deployment-id}/chat/completions?api-version=2023-05-15

        if (config.getApiEndpoint() == null) {
            throw new IllegalArgumentException("Azure OpenAI requires apiEndpoint to be configured");
        }

        JsonObject requestBody = new JsonObject()
                .put("messages", new JsonArray()
                        .add(new JsonObject()
                                .put("role", "system")
                                .put("content", config.getParameters() != null &&
                                        config.getParameters().getSystemPrompt() != null
                                                ? config.getParameters().getSystemPrompt()
                                                : "You are a helpful assistant."))
                        .add(new JsonObject()
                                .put("role", "user")
                                .put("content", prompt)));

        if (config.getParameters() != null) {
            if (config.getParameters().getTemperature() != null) {
                requestBody.put("temperature", config.getParameters().getTemperature());
            }
            if (config.getParameters().getMaxTokens() != null) {
                requestBody.put("max_tokens", config.getParameters().getMaxTokens());
            }
            if (config.getParameters().getTopP() != null) {
                requestBody.put("top_p", config.getParameters().getTopP());
            }
            if (config.getParameters().getFrequencyPenalty() != null) {
                requestBody.put("frequency_penalty", config.getParameters().getFrequencyPenalty());
            }
            if (config.getParameters().getPresencePenalty() != null) {
                requestBody.put("presence_penalty", config.getParameters().getPresencePenalty());
            }
        }

        LOG.debugf("Calling Azure OpenAI: %s", config.getApiEndpoint());

        AzureOpenAIClient client = RestClientBuilder.newBuilder()
                .baseUri(URI.create(config.getApiEndpoint()))
                .build(AzureOpenAIClient.class);

        return client.getCompletions(config.getApiKey(), requestBody)
                .map(this::extractResponse);
    }

    private String extractResponse(JsonObject body) {
        return body.getJsonArray("choices")
                .getJsonObject(0)
                .getJsonObject("message")
                .getString("content");
    }

    @Override
    public LLMConfig.Provider getSupportedProvider() {
        return LLMConfig.Provider.AZURE;
    }
}
