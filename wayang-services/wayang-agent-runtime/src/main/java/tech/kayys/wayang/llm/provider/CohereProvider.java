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
 * Cohere Provider
 */
@ApplicationScoped
public class CohereProvider implements LLMProvider {

    private static final Logger LOG = Logger.getLogger(CohereProvider.class);
    private static final String DEFAULT_ENDPOINT = "https://api.cohere.ai/v1/generate";

    @Override
    public Uni<String> complete(LLMConfig config, String prompt, ExecutionContext context) {
        String endpoint = config.getApiEndpoint() != null ? config.getApiEndpoint() : DEFAULT_ENDPOINT;

        JsonObject requestBody = new JsonObject()
                .put("model", config.getModel() != null ? config.getModel() : "command")
                .put("prompt", prompt)
                .put("max_tokens", config.getParameters() != null && config.getParameters().getMaxTokens() != null
                        ? config.getParameters().getMaxTokens()
                        : 1000);

        if (config.getParameters() != null) {
            if (config.getParameters().getTemperature() != null) {
                requestBody.put("temperature", config.getParameters().getTemperature());
            }
            if (config.getParameters().getTopP() != null) {
                requestBody.put("p", config.getParameters().getTopP());
            }
            if (config.getParameters().getTopK() != null) {
                requestBody.put("k", config.getParameters().getTopK());
            }
            if (config.getParameters().getStopSequences() != null) {
                requestBody.put("stop_sequences", new JsonArray(config.getParameters().getStopSequences()));
            }
        }

        LOG.debugf("Calling Cohere API with model: %s", config.getModel());

        CohereClient client = RestClientBuilder.newBuilder()
                .baseUri(URI.create(endpoint))
                .build(CohereClient.class);

        return client.generate("Bearer " + config.getApiKey(), requestBody)
                .map(this::extractResponse);
    }

    private String extractResponse(JsonObject body) {
        JsonArray generations = body.getJsonArray("generations");

        if (generations != null && generations.size() > 0) {
            return generations.getJsonObject(0).getString("text");
        }

        throw new RuntimeException("No content in Cohere response");
    }

    @Override
    public LLMConfig.Provider getSupportedProvider() {
        return LLMConfig.Provider.COHERE;
    }
}