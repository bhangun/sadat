package tech.kayys.wayang.agent.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.exception.AuthenticationException;
import tech.kayys.wayang.agent.exception.RateLimitException;
import tech.kayys.wayang.agent.model.AbstractLLMProvider;
import tech.kayys.wayang.agent.model.LLMRequest;
import tech.kayys.wayang.agent.model.LLMResponse;
import tech.kayys.wayang.agent.model.Message;
import tech.kayys.wayang.agent.model.TokenUsage;
import tech.kayys.wayang.agent.model.ToolCall;
import tech.kayys.wayang.agent.model.ToolDefinition;

@ApplicationScoped
public class RealAnthropicProvider extends AbstractLLMProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RealAnthropicProvider.class);

    @Inject
    WebClient webClient;

    @ConfigProperty(name = "silat.agent.llm.anthropic.api-key")
    String apiKey;

    @ConfigProperty(name = "silat.agent.llm.anthropic.base-url", defaultValue = "https://api.anthropic.com/v1")
    String baseUrl;

    @ConfigProperty(name = "silat.agent.llm.anthropic.timeout", defaultValue = "60000")
    long timeout;

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    public RealAnthropicProvider() {
        super("anthropic", Map.of());
    }

    @Override
    public Uni<LLMResponse> complete(LLMRequest request) {
        validateRequest(request);

        LOG.debug("Anthropic API call: model={}, messages={}",
                request.model(), request.messages().size());

        JsonObject requestBody = buildRequestBody(request);

        return Uni.createFrom().completionStage(webClient
                .postAbs(baseUrl + "/messages")
                .putHeader("x-api-key", apiKey)
                .putHeader("anthropic-version", ANTHROPIC_VERSION)
                .putHeader("Content-Type", "application/json")
                .timeout(timeout)
                .sendJsonObject(requestBody)
                .toCompletionStage())
                .onItem().transform(this::handleResponse)
                .onFailure().retry()
                .withBackOff(Duration.ofSeconds(1), Duration.ofSeconds(10))
                .atMost(3)
                .onFailure().transform(this::handleError);
    }

    @Override
    public io.smallrye.mutiny.Multi<String> stream(LLMRequest request) {
        validateRequest(request);

        JsonObject requestBody = buildRequestBody(request);
        requestBody.put("stream", true);

        return Uni.createFrom().completionStage(webClient
                .postAbs(baseUrl + "/messages")
                .putHeader("x-api-key", apiKey)
                .putHeader("anthropic-version", ANTHROPIC_VERSION)
                .putHeader("Content-Type", "application/json")
                .timeout(timeout)
                .sendJsonObject(requestBody)
                .toCompletionStage())
                .onItem().transformToMulti(response -> parseStreamResponse(response));
    }

    @Override
    public boolean supportsFunctionCalling() {
        return true;
    }

    @Override
    public List<String> supportedModels() {
        return List.of(
                "claude-3-opus-20240229",
                "claude-3-sonnet-20240229",
                "claude-3-haiku-20240307",
                "claude-3-5-sonnet-20241022",
                "claude-2.1",
                "claude-2.0");
    }

    private JsonObject buildRequestBody(LLMRequest request) {
        // Extract system message
        String systemMessage = request.messages().stream()
                .filter(Message::isSystem)
                .map(Message::content)
                .findFirst()
                .orElse(null);

        // Get non-system messages
        List<Message> userMessages = request.messages().stream()
                .filter(msg -> !msg.isSystem())
                .collect(Collectors.toList());

        JsonObject body = new JsonObject()
                .put("model", request.model())
                .put("max_tokens", request.maxTokens())
                .put("messages", convertMessages(userMessages));

        if (systemMessage != null) {
            body.put("system", systemMessage);
        }

        if (request.temperature() != null) {
            body.put("temperature", request.temperature());
        }

        if (!request.tools().isEmpty()) {
            body.put("tools", convertTools(request.tools()));
        }

        return body;
    }

    @Override
    protected Object convertMessages(List<Message> messages) {
        JsonArray jsonMessages = new JsonArray();

        for (Message msg : messages) {
            JsonObject jsonMsg = new JsonObject()
                    .put("role", msg.role());

            // Anthropic uses content array format
            JsonArray content = new JsonArray();

            if (msg.content() != null) {
                content.add(new JsonObject()
                        .put("type", "text")
                        .put("text", msg.content()));
            }

            // Add tool use blocks
            if (msg.hasToolCalls()) {
                for (ToolCall tc : msg.toolCalls()) {
                    content.add(new JsonObject()
                            .put("type", "tool_use")
                            .put("id", tc.id())
                            .put("name", tc.name())
                            .put("input", new JsonObject(tc.arguments())));
                }
            }

            // Add tool result blocks
            if (msg.toolCallId() != null) {
                content.add(new JsonObject()
                        .put("type", "tool_result")
                        .put("tool_use_id", msg.toolCallId())
                        .put("content", msg.content()));
            }

            jsonMsg.put("content", content);
            jsonMessages.add(jsonMsg);
        }

        return jsonMessages;
    }

    @Override
    protected Object convertTools(List<ToolDefinition> tools) {
        JsonArray jsonTools = new JsonArray();

        for (ToolDefinition tool : tools) {
            jsonTools.add(new JsonObject()
                    .put("name", tool.name())
                    .put("description", tool.description())
                    .put("input_schema", new JsonObject(tool.parameters())));
        }

        return jsonTools;
    }

    @Override
    protected LLMResponse parseResponse(Object response) {
        JsonObject json = (JsonObject) response;
        JsonArray content = json.getJsonArray("content");

        StringBuilder textContent = new StringBuilder();
        List<ToolCall> toolCalls = new ArrayList<>();

        for (int i = 0; i < content.size(); i++) {
            JsonObject block = content.getJsonObject(i);
            String type = block.getString("type");

            if ("text".equals(type)) {
                textContent.append(block.getString("text"));
            } else if ("tool_use".equals(type)) {
                toolCalls.add(ToolCall.create(
                        block.getString("id"),
                        block.getString("name"),
                        block.getJsonObject("input").getMap()));
            }
        }

        JsonObject usage = json.getJsonObject("usage");
        TokenUsage tokenUsage = TokenUsage.of(
                usage.getInteger("input_tokens"),
                usage.getInteger("output_tokens"));

        String finishReason = json.getString("stop_reason");

        if (!toolCalls.isEmpty()) {
            return LLMResponse.withToolCalls(
                    textContent.toString(), toolCalls, tokenUsage);
        } else {
            return LLMResponse.create(
                    textContent.toString(), finishReason, tokenUsage);
        }
    }

    private LLMResponse handleResponse(HttpResponse<Buffer> response) {
        if (response.statusCode() != 200) {
            String error = response.bodyAsString();
            LOG.error("Anthropic API error: {} - {}", response.statusCode(), error);
            throw new RuntimeException("Anthropic API error: " + response.statusCode());
        }

        JsonObject json = response.bodyAsJsonObject();
        return parseResponse(json);
    }

    private Throwable handleError(Throwable error) {
        LOG.error("Anthropic API request failed", error);

        if (error.getMessage().contains("429")) {
            return new RateLimitException("Anthropic rate limit exceeded");
        } else if (error.getMessage().contains("401")) {
            return new AuthenticationException("Invalid Anthropic API key");
        }

        return error;
    }

    private io.smallrye.mutiny.Multi<String> parseStreamResponse(
            HttpResponse<Buffer> response) {

        if (response.statusCode() != 200) {
            return io.smallrye.mutiny.Multi.createFrom()
                    .failure(new RuntimeException("API error: " + response.statusCode()));
        }

        // Parse SSE stream
        String body = response.bodyAsString();
        return parseSSEStream(body);
    }

    private io.smallrye.mutiny.Multi<String> parseSSEStream(String sseData) {
        List<String> chunks = Arrays.asList(sseData.split("\n\n"));

        return io.smallrye.mutiny.Multi.createFrom().items(chunks.stream())
                .filter(chunk -> chunk.startsWith("data: "))
                .map(chunk -> chunk.substring(6))
                .filter(data -> !data.equals("[DONE]"))
                .map(data -> {
                    JsonObject json = new JsonObject(data);
                    String type = json.getString("type");

                    if ("content_block_delta".equals(type)) {
                        JsonObject delta = json.getJsonObject("delta");
                        return delta.getString("text", "");
                    }
                    return "";
                })
                .filter(content -> !content.isEmpty());
    }
}
