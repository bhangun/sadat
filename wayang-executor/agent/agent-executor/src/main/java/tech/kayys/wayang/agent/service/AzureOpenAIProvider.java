package tech.kayys.wayang.agent.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.model.AbstractLLMProvider;
import tech.kayys.wayang.agent.model.LLMRequest;
import tech.kayys.wayang.agent.model.LLMResponse;
import tech.kayys.wayang.agent.model.Message;
import tech.kayys.wayang.agent.model.ToolDefinition;

@ApplicationScoped
public class AzureOpenAIProvider extends AbstractLLMProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AzureOpenAIProvider.class);

    @Inject
    WebClient webClient;

    @ConfigProperty(name = "silat.agent.llm.azure.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "silat.agent.llm.azure.endpoint")
    Optional<String> endpoint;

    @ConfigProperty(name = "silat.agent.llm.azure.deployment")
    Optional<String> deployment;

    @ConfigProperty(name = "silat.agent.llm.azure.api-version", defaultValue = "2024-02-15-preview")
    String apiVersion;

    public AzureOpenAIProvider() {
        super("azure-openai", Map.of());
    }

    @Override
    public Uni<LLMResponse> complete(LLMRequest request) {
        if (apiKey.isEmpty() || endpoint.isEmpty() || deployment.isEmpty()) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Azure OpenAI not configured"));
        }

        validateRequest(request);

        String url = String.format("%s/openai/deployments/%s/chat/completions?api-version=%s",
                endpoint.get(), deployment.get(), apiVersion);

        JsonObject requestBody = buildRequestBody(request);

        return webClient
                .postAbs(url)
                .putHeader("api-key", apiKey.get())
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(requestBody)
                .onItem().transform(this::handleResponse)
                .onFailure().retry().atMost(3)
                .onFailure().transform(this::handleError);
    }

    @Override
    public io.smallrye.mutiny.Multi<String> stream(LLMRequest request) {
        // Similar to OpenAI streaming implementation
        return io.smallrye.mutiny.Multi.createFrom()
                .failure(new UnsupportedOperationException("Streaming not yet implemented"));
    }

    @Override
    public boolean supportsFunctionCalling() {
        return true;
    }

    @Override
    public List<String> supportedModels() {
        // Azure uses deployment names, not model names
        return List.of("azure-deployment");
    }

    private JsonObject buildRequestBody(LLMRequest request) {
        // Same as OpenAI format
        return new JsonObject()
                .put("messages", convertMessages(request.messages()))
                .put("temperature", request.temperature())
                .put("max_tokens", request.maxTokens());
    }

    @Override
    protected Object convertMessages(List<Message> messages) {
        // Same as OpenAI
        return new RealOpenAIProvider().convertMessages(messages);
    }

    @Override
    protected Object convertTools(List<ToolDefinition> tools) {
        // Same as OpenAI
        return new RealOpenAIProvider().convertTools(tools);
    }

    @Override
    protected LLMResponse parseResponse(Object response) {
        // Same as OpenAI
        return new RealOpenAIProvider().parseResponse(response);
    }

    private LLMResponse handleResponse(HttpResponse<Buffer> response) {
        if (response.statusCode() != 200) {
            throw new RuntimeException("Azure OpenAI error: " + response.statusCode());
        }
        return parseResponse(response.bodyAsJsonObject());
    }

    private Throwable handleError(Throwable error) {
        LOG.error("Azure OpenAI request failed", error);
        return error;
    }
}