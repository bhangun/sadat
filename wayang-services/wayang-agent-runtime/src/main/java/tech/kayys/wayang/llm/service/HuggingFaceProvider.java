package tech.kayys.wayang.llm.service;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.model.LLMConfig;
import tech.kayys.wayang.llm.model.LLMProvider;
import tech.kayys.wayang.workflow.model.ExecutionContext;

import org.jboss.logging.Logger;

@ApplicationScoped
public class HuggingFaceProvider implements LLMProvider {

    private static final Logger LOG = Logger.getLogger(HuggingFaceProvider.class);
    private static final String DEFAULT_ENDPOINT = "https://api-inference.huggingface.co/models";

    @Inject
    Vertx vertx;

    private WebClient webClient;

    @jakarta.annotation.PostConstruct
    void init() {
        webClient = WebClient.create(vertx);
    }

    @Override
    public Uni<String> complete(LLMConfig config, String prompt, ExecutionContext context) {
        String model = config.getModel() != null ? config.getModel() : "meta-llama/Llama-2-7b-chat-hf";
        String endpoint = config.getApiEndpoint() != null ? config.getApiEndpoint() : DEFAULT_ENDPOINT + "/" + model;

        JsonObject requestBody = new JsonObject()
                .put("inputs", prompt);

        // Add parameters
        JsonObject parameters = new JsonObject();
        if (config.getParameters() != null) {
            if (config.getParameters().getTemperature() != null) {
                parameters.put("temperature", config.getParameters().getTemperature());
            }
            if (config.getParameters().getMaxTokens() != null) {
                parameters.put("max_new_tokens", config.getParameters().getMaxTokens());
            }
            if (config.getParameters().getTopP() != null) {
                parameters.put("top_p", config.getParameters().getTopP());
            }
            if (config.getParameters().getTopK() != null) {
                parameters.put("top_k", config.getParameters().getTopK());
            }
        }

        if (!parameters.isEmpty()) {
            requestBody.put("parameters", parameters);
        }

        LOG.debugf("Calling HuggingFace API with model: %s", model);

        return webClient.postAbs(endpoint)
                .putHeader("Authorization", "Bearer " + config.getApiKey())
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(requestBody)
                .map(response -> {
                    if (response.statusCode() != 200) {
                        throw new RuntimeException("HuggingFace API error: " +
                                response.statusCode() + " - " + response.bodyAsString());
                    }

                    JsonArray body = response.bodyAsJsonArray();
                    if (body != null && body.size() > 0) {
                        JsonObject result = body.getJsonObject(0);
                        return result.getString("generated_text", "");
                    }

                    throw new RuntimeException("No content in HuggingFace response");
                });
    }

    @Override
    public LLMConfig.Provider getSupportedProvider() {
        return LLMConfig.Provider.HUGGINGFACE;
    }
}
