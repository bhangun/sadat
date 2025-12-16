package tech.kayys.wayang.llm.provider;

import org.jboss.logging.Logger;
import io.smallrye.mutiny.Uni;

import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import java.net.URI;
import tech.kayys.wayang.agent.model.LLMConfig;
import tech.kayys.wayang.llm.model.LLMProvider;
import tech.kayys.wayang.workflow.model.ExecutionContext;

/**
 * Ollama Provider (for local models)
 */
@ApplicationScoped
public class OllamaProvider implements LLMProvider {

    private static final Logger LOG = Logger.getLogger(OllamaProvider.class);
    private static final String DEFAULT_ENDPOINT = "http://localhost:11434/api/generate";

    @Override
    public Uni<String> complete(LLMConfig config, String prompt, ExecutionContext context) {
        String endpoint = config.getApiEndpoint() != null ? config.getApiEndpoint() : DEFAULT_ENDPOINT;

        JsonObject requestBody = new JsonObject()
                .put("model", config.getModel())
                .put("prompt", prompt)
                .put("stream", false);

        JsonObject options = new JsonObject();
        if (config.getParameters() != null) {
            if (config.getParameters().getTemperature() != null) {
                options.put("temperature", config.getParameters().getTemperature());
            }
            if (config.getParameters().getTopP() != null) {
                options.put("top_p", config.getParameters().getTopP());
            }
            if (config.getParameters().getTopK() != null) {
                options.put("top_k", config.getParameters().getTopK());
            }
        }

        if (!options.isEmpty()) {
            requestBody.put("options", options);
        }

        // Add system prompt
        if (config.getParameters() != null && config.getParameters().getSystemPrompt() != null) {
            requestBody.put("system", config.getParameters().getSystemPrompt());
        }

        LOG.debugf("Calling Ollama with model: %s", config.getModel());

        OllamaClient client = RestClientBuilder.newBuilder()
                .baseUri(URI.create(endpoint))
                .build(OllamaClient.class);

        return client.generate(requestBody)
                .map(this::extractResponse);
    }

    private String extractResponse(JsonObject body) {
        return body.getString("response");
    }

    @Override
    public LLMConfig.Provider getSupportedProvider() {
        return LLMConfig.Provider.OLLAMA;
    }
}
