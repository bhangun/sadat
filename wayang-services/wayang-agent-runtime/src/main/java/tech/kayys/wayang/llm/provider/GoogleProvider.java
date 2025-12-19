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
 * Google Gemini Provider
 */
@ApplicationScoped
public class GoogleProvider implements LLMProvider {

    private static final Logger LOG = Logger.getLogger(GoogleProvider.class);
    private static final String DEFAULT_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models";

    @Override
    public Uni<String> complete(LLMConfig config, String prompt, ExecutionContext context) {
        String model = config.getModel() != null ? config.getModel() : "gemini-pro";
        String endpoint = config.getApiEndpoint() != null ? config.getApiEndpoint() : DEFAULT_ENDPOINT;

        JsonObject requestBody = new JsonObject()
                .put("contents", new JsonArray()
                        .add(new JsonObject()
                                .put("parts", new JsonArray()
                                        .add(new JsonObject()
                                                .put("text", prompt)))));

        // Add generation config
        JsonObject generationConfig = new JsonObject();
        if (config.getParameters() != null) {
            if (config.getParameters().getTemperature() != null) {
                generationConfig.put("temperature", config.getParameters().getTemperature());
            }
            if (config.getParameters().getMaxTokens() != null) {
                generationConfig.put("maxOutputTokens", config.getParameters().getMaxTokens());
            }
            if (config.getParameters().getTopP() != null) {
                generationConfig.put("topP", config.getParameters().getTopP());
            }
            if (config.getParameters().getTopK() != null) {
                generationConfig.put("topK", config.getParameters().getTopK());
            }
        }

        if (!generationConfig.isEmpty()) {
            requestBody.put("generationConfig", generationConfig);
        }

        LOG.debugf("Calling Google Gemini API with model: %s", model);

        GoogleClient client = RestClientBuilder.newBuilder()
                .baseUri(URI.create(endpoint))
                .build(GoogleClient.class);

        return client.generateContent(model, config.getApiKey(), requestBody)
                .map(this::extractResponse);
    }

    private String extractResponse(JsonObject body) {
        JsonArray candidates = body.getJsonArray("candidates");

        if (candidates != null && candidates.size() > 0) {
            JsonObject candidate = candidates.getJsonObject(0);
            JsonObject content = candidate.getJsonObject("content");
            JsonArray parts = content.getJsonArray("parts");

            if (parts != null && parts.size() > 0) {
                return parts.getJsonObject(0).getString("text");
            }
        }

        throw new RuntimeException("No content in Google response");
    }

    @Override
    public LLMConfig.Provider getSupportedProvider() {
        return LLMConfig.Provider.GOOGLE;
    }
}