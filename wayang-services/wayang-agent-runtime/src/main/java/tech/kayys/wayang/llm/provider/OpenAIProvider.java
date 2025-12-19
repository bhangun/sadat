package tech.kayys.wayang.llm.provider;

import org.jboss.logging.Logger;

import io.smallrye.mutiny.Uni;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import java.net.URI;
import tech.kayys.wayang.agent.model.LLMConfig;
import tech.kayys.wayang.llm.model.LLMProvider;
import tech.kayys.wayang.workflow.model.ExecutionContext;

/**
 * OpenAI Provider Implementation
 */
@ApplicationScoped
public class OpenAIProvider implements LLMProvider {

    private static final Logger LOG = Logger.getLogger(OpenAIProvider.class);

    @Override
    public Uni<String> complete(LLMConfig config, String prompt, ExecutionContext context) {
        String endpoint = config.getApiEndpoint() != null ? config.getApiEndpoint()
                : "https://api.openai.com/v1/chat/completions";

        JsonObject requestBody = new JsonObject()
                .put("model", config.getModel())
                .put("messages", new JsonArray()
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
        }

        LOG.debugf("Calling OpenAI with model: %s", config.getModel());

        OpenAIClient client = RestClientBuilder.newBuilder()
                .baseUri(URI.create(endpoint))
                .build(OpenAIClient.class);

        return client.chatCompletions("Bearer " + config.getApiKey(), requestBody)
                .map(response -> {
                    JsonObject body = response;
                    return body.getJsonArray("choices")
                            .getJsonObject(0)
                            .getJsonObject("message")
                            .getString("content");
                });
    }

    @Override
    public LLMConfig.Provider getSupportedProvider() {
        return LLMConfig.Provider.OPENAI;
    }
}
