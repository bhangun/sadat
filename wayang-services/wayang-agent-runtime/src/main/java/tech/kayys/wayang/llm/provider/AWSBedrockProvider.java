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
 * AWS Bedrock Provider
 */
@ApplicationScoped
public class AWSBedrockProvider implements LLMProvider {

    private static final Logger LOG = Logger.getLogger(AWSBedrockProvider.class);

    @Override
    public Uni<String> complete(LLMConfig config, String prompt, ExecutionContext context) {
        // AWS Bedrock requires AWS Signature V4 authentication
        // This is a simplified version - production should use AWS SDK

        String endpoint = config.getApiEndpoint();
        if (endpoint == null) {
            throw new IllegalArgumentException(
                    "AWS Bedrock requires apiEndpoint (e.g., bedrock-runtime.us-east-1.amazonaws.com)");
        }

        // Different models have different request formats
        JsonObject requestBody = buildBedrockRequest(config.getModel(), prompt, config.getParameters());

        LOG.debugf("Calling AWS Bedrock with model: %s", config.getModel());

        String baseUri = "https://" + endpoint;
        AWSBedrockClient client = RestClientBuilder.newBuilder()
                .baseUri(URI.create(baseUri))
                .build(AWSBedrockClient.class);

        // Note: In production, use AWS SDK for proper authentication
        // AWS Signature V4 headers would go here (not implemented in this simplified
        // version)
        return client.invokeModel(config.getModel(), requestBody)
                .map(response -> extractBedrockResponse(response, config.getModel()));
    }

    private JsonObject buildBedrockRequest(String model, String prompt, LLMConfig.Parameters params) {
        JsonObject request = new JsonObject();

        // Anthropic Claude on Bedrock
        if (model.startsWith("anthropic.claude")) {
            request.put("prompt", "\n\nHuman: " + prompt + "\n\nAssistant:");
            request.put("max_tokens_to_sample", params != null && params.getMaxTokens() != null
                    ? params.getMaxTokens()
                    : 2000);
            if (params != null && params.getTemperature() != null) {
                request.put("temperature", params.getTemperature());
            }
        }
        // Amazon Titan
        else if (model.startsWith("amazon.titan")) {
            JsonObject inputText = new JsonObject()
                    .put("inputText", prompt);

            JsonObject textGenerationConfig = new JsonObject()
                    .put("maxTokenCount", params != null && params.getMaxTokens() != null
                            ? params.getMaxTokens()
                            : 2000);

            if (params != null && params.getTemperature() != null) {
                textGenerationConfig.put("temperature", params.getTemperature());
            }

            request.mergeIn(inputText);
            request.put("textGenerationConfig", textGenerationConfig);
        }

        return request;
    }

    private String extractBedrockResponse(JsonObject body, String model) {
        // Different models return different response formats
        if (model.startsWith("anthropic.claude")) {
            return body.getString("completion");
        } else if (model.startsWith("amazon.titan")) {
            JsonArray results = body.getJsonArray("results");
            if (results != null && results.size() > 0) {
                return results.getJsonObject(0).getString("outputText");
            }
        }

        throw new RuntimeException("Unknown response format from AWS Bedrock");
    }

    @Override
    public LLMConfig.Provider getSupportedProvider() {
        return LLMConfig.Provider.AWS;
    }
}