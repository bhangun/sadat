package tech.kayys.wayang.agent.service;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.exception.AuthenticationException;
import tech.kayys.wayang.agent.exception.RateLimitException;
import tech.kayys.wayang.agent.exception.TimeoutException;
import tech.kayys.wayang.agent.model.AbstractLLMProvider;
import tech.kayys.wayang.agent.model.LLMRequest;
import tech.kayys.wayang.agent.model.LLMResponse;
import tech.kayys.wayang.agent.model.Message;
import tech.kayys.wayang.agent.model.TokenUsage;
import tech.kayys.wayang.agent.model.ToolCall;
import tech.kayys.wayang.agent.model.ToolDefinition;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * REAL LLM HTTP CLIENT IMPLEMENTATIONS
 * ============================================================================
 * 
 * Production-ready HTTP clients for LLM providers using Vert.x WebClient
 * 
 * Features:
 * - Proper HTTP/2 support
 * - Connection pooling
 * - Request/response streaming
 * - Error handling and retries
 * - Rate limit handling
 * - Request/response logging
 */

// ==================== REAL OPENAI PROVIDER ====================

@ApplicationScoped
public class RealOpenAIProvider extends AbstractLLMProvider {

    private static final Logger LOG = LoggerFactory.getLogger(RealOpenAIProvider.class);

    @Inject
    WebClient webClient;

    @ConfigProperty(name = "silat.agent.llm.openai.api-key")
    String apiKey;

    @ConfigProperty(name = "silat.agent.llm.openai.base-url", defaultValue = "https://api.openai.com/v1")
    String baseUrl;

    @ConfigProperty(name = "silat.agent.llm.openai.timeout", defaultValue = "60000")
    long timeout;

    public RealOpenAIProvider() {
        super("openai", Map.of());
    }

    @Override
    public Uni<LLMResponse> complete(LLMRequest request) {
        validateRequest(request);

        LOG.debug("OpenAI API call: model={}, messages={}, tools={}",
                request.model(), request.messages().size(), request.tools().size());

        JsonObject requestBody = buildRequestBody(request);

        return webClient
                .postAbs(baseUrl + "/chat/completions")
                .putHeader("Authorization", "Bearer " + apiKey)
                .putHeader("Content-Type", "application/json")
                .timeout(timeout)
                .sendJsonObject(requestBody)
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

        return webClient
                .postAbs(baseUrl + "/chat/completions")
                .putHeader("Authorization", "Bearer " + apiKey)
                .putHeader("Content-Type", "application/json")
                .timeout(timeout)
                .sendJsonObject(requestBody)
                .onItem().transformToMulti(response -> {
                    if (response.statusCode() != 200) {
                        return io.smallrye.mutiny.Multi.createFrom()
                                .failure(new RuntimeException("API error: " + response.statusCode()));
                    }

                    // Parse SSE stream
                    return parseSSEStream(response.bodyAsString());
                });
    }

    @Override
    public boolean supportsFunctionCalling() {
        return true;
    }

    @Override
    public List<String> supportedModels() {
        return List.of(
                "gpt-4-turbo-preview",
                "gpt-4-0125-preview",
                "gpt-4",
                "gpt-4-32k",
                "gpt-3.5-turbo",
                "gpt-3.5-turbo-16k",
                "gpt-4o",
                "gpt-4o-mini");
    }

    private JsonObject buildRequestBody(LLMRequest request) {
        JsonObject body = new JsonObject()
                .put("model", request.model())
                .put("messages", convertMessages(request.messages()))
                .put("temperature", request.temperature())
                .put("max_tokens", request.maxTokens());

        if (!request.tools().isEmpty()) {
            body.put("tools", convertTools(request.tools()));
            body.put("tool_choice", "auto");
        }

        // Add additional params
        request.additionalParams().forEach(body::put);

        return body;
    }

    @Override
    protected Object convertMessages(List<Message> messages) {
        JsonArray jsonMessages = new JsonArray();

        for (Message msg : messages) {
            JsonObject jsonMsg = new JsonObject()
                    .put("role", msg.role());

            if (msg.content() != null) {
                jsonMsg.put("content", msg.content());
            }

            if (msg.hasToolCalls()) {
                JsonArray toolCalls = new JsonArray();
                for (ToolCall tc : msg.toolCalls()) {
                    toolCalls.add(new JsonObject()
                            .put("id", tc.id())
                            .put("type", "function")
                            .put("function", new JsonObject()
                                    .put("name", tc.name())
                                    .put("arguments", new JsonObject(tc.arguments()).encode())));
                }
                jsonMsg.put("tool_calls", toolCalls);
            }

            if (msg.toolCallId() != null) {
                jsonMsg.put("tool_call_id", msg.toolCallId());
            }

            jsonMessages.add(jsonMsg);
        }

        return jsonMessages;
    }

    @Override
    protected Object convertTools(List<ToolDefinition> tools) {
        JsonArray jsonTools = new JsonArray();

        for (ToolDefinition tool : tools) {
            jsonTools.add(new JsonObject()
                    .put("type", "function")
                    .put("function", new JsonObject()
                            .put("name", tool.name())
                            .put("description", tool.description())
                            .put("parameters", new JsonObject(tool.parameters()))));
        }

        return jsonTools;
    }

    @Override
    protected LLMResponse parseResponse(Object response) {
        JsonObject json = (JsonObject) response;
        JsonArray choices = json.getJsonArray("choices");
        JsonObject firstChoice = choices.getJsonObject(0);
        JsonObject message = firstChoice.getJsonObject("message");

        String content = message.getString("content", "");
        String finishReason = firstChoice.getString("finish_reason");

        // Parse tool calls
        List<ToolCall> toolCalls = new ArrayList<>();
        JsonArray toolCallsArray = message.getJsonArray("tool_calls");
        if (toolCallsArray != null) {
            for (int i = 0; i < toolCallsArray.size(); i++) {
                JsonObject tc = toolCallsArray.getJsonObject(i);
                JsonObject function = tc.getJsonObject("function");

                Map<String, Object> arguments = new JsonObject(function.getString("arguments")).getMap();

                toolCalls.add(ToolCall.create(
                        tc.getString("id"),
                        function.getString("name"),
                        arguments));
            }
        }

        // Parse usage
        JsonObject usage = json.getJsonObject("usage");
        TokenUsage tokenUsage = TokenUsage.of(
                usage.getInteger("prompt_tokens"),
                usage.getInteger("completion_tokens"));

        if (!toolCalls.isEmpty()) {
            return LLMResponse.withToolCalls(content, toolCalls, tokenUsage);
        } else {
            return LLMResponse.create(content, finishReason, tokenUsage);
        }
    }

    private LLMResponse handleResponse(HttpResponse<Buffer> response) {
        if (response.statusCode() != 200) {
            String error = response.bodyAsString();
            LOG.error("OpenAI API error: {} - {}", response.statusCode(), error);
            throw new RuntimeException("OpenAI API error: " + response.statusCode());
        }

        JsonObject json = response.bodyAsJsonObject();
        return parseResponse(json);
    }

    private Throwable handleError(Throwable error) {
        LOG.error("OpenAI API request failed", error);

        if (error.getMessage().contains("429")) {
            return new RateLimitException("OpenAI rate limit exceeded");
        } else if (error.getMessage().contains("401")) {
            return new AuthenticationException("Invalid OpenAI API key");
        } else if (error.getMessage().contains("timeout")) {
            return new TimeoutException("OpenAI API timeout");
        }

        return error;
    }

    private io.smallrye.mutiny.Multi<String> parseSSEStream(String sseData) {
        // Parse Server-Sent Events format
        List<String> chunks = Arrays.asList(sseData.split("\n\n"));

        return io.smallrye.mutiny.Multi.createFrom().items(chunks.stream())
                .filter(chunk -> chunk.startsWith("data: "))
                .map(chunk -> chunk.substring(6))
                .filter(data -> !data.equals("[DONE]"))
                .map(data -> {
                    JsonObject json = new JsonObject(data);
                    JsonArray choices = json.getJsonArray("choices");
                    if (choices != null && choices.size() > 0) {
                        JsonObject delta = choices.getJsonObject(0)
                                .getJsonObject("delta");
                        return delta.getString("content", "");
                    }
                    return "";
                })
                .filter(content -> !content.isEmpty());
    }
}
