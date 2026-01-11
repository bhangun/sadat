package tech.kayys.wayang.agent.model.llmprovider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.model.AbstractLLMProvider;
import tech.kayys.wayang.agent.model.LLMRequest;
import tech.kayys.wayang.agent.model.LLMResponse;
import tech.kayys.wayang.agent.model.Message;
import tech.kayys.wayang.agent.model.TokenUsage;
import tech.kayys.wayang.agent.model.ToolCall;
import tech.kayys.wayang.agent.model.ToolDefinition;

@ApplicationScoped
public class OpenAIProvider extends AbstractLLMProvider {

    private static final Logger LOG = LoggerFactory.getLogger(OpenAIProvider.class);

    private final String apiKey;
    private final String baseUrl;

    public OpenAIProvider() {
        super("openai", Map.of(
                "baseUrl", "https://api.openai.com/v1",
                "timeout", 60));

        // Load from environment or configuration
        this.apiKey = System.getenv("OPENAI_API_KEY");
        this.baseUrl = config.getOrDefault("baseUrl",
                "https://api.openai.com/v1").toString();
    }

    @Override
    public Uni<LLMResponse> complete(LLMRequest request) {
        validateRequest(request);

        LOG.debug("OpenAI completion: model={}, messages={}, tools={}",
                request.model(), request.messages().size(), request.tools().size());

        return Uni.createFrom().deferred(() -> {
            try {
                // Build OpenAI API request
                Map<String, Object> apiRequest = buildApiRequest(request);

                // Call OpenAI API
                return callOpenAIAPI(apiRequest)
                        .map(this::parseResponse)
                        .onItem().invoke(response -> LOG.debug("OpenAI response: tokens={}",
                                response.usage().totalTokens()));

            } catch (Exception e) {
                LOG.error("OpenAI API error: {}", e.getMessage(), e);
                return Uni.createFrom().failure(e);
            }
        });
    }

    @Override
    public io.smallrye.mutiny.Multi<String> stream(LLMRequest request) {
        validateRequest(request);

        // Streaming implementation
        return io.smallrye.mutiny.Multi.createFrom().items(
                "Streaming not yet implemented for OpenAI");
    }

    @Override
    public boolean supportsFunctionCalling() {
        return true;
    }

    @Override
    public List<String> supportedModels() {
        return List.of(
                "gpt-4",
                "gpt-4-turbo",
                "gpt-4o",
                "gpt-3.5-turbo",
                "gpt-3.5-turbo-16k");
    }

    private Map<String, Object> buildApiRequest(LLMRequest request) {
        Map<String, Object> apiRequest = new HashMap<>();

        apiRequest.put("model", request.model());
        apiRequest.put("messages", convertMessages(request.messages()));
        apiRequest.put("temperature", request.temperature());
        apiRequest.put("max_tokens", request.maxTokens());

        if (!request.tools().isEmpty()) {
            apiRequest.put("tools", convertTools(request.tools()));
            apiRequest.put("tool_choice", "auto");
        }

        if (request.streaming()) {
            apiRequest.put("stream", true);
        }

        return apiRequest;
    }

    @Override
    protected Object convertMessages(List<Message> messages) {
        // Convert to OpenAI message format
        return messages.stream()
                .map(msg -> {
                    Map<String, Object> apiMsg = new HashMap<>();
                    apiMsg.put("role", msg.role());
                    apiMsg.put("content", msg.content());

                    if (msg.hasToolCalls()) {
                        apiMsg.put("tool_calls", convertToolCalls(msg.toolCalls()));
                    }

                    if (msg.toolCallId() != null) {
                        apiMsg.put("tool_call_id", msg.toolCallId());
                    }

                    return apiMsg;
                })
                .toList();
    }

    private List<Map<String, Object>> convertToolCalls(List<ToolCall> toolCalls) {
        return toolCalls.stream()
                .map(tc -> Map.of(
                        "id", tc.id(),
                        "type", "function",
                        "function", Map.of(
                                "name", tc.name(),
                                "arguments", tc.arguments())))
                .toList();
    }

    @Override
    protected Object convertTools(List<ToolDefinition> tools) {
        return tools.stream()
                .map(tool -> Map.of(
                        "type", "function",
                        "function", Map.of(
                                "name", tool.name(),
                                "description", tool.description(),
                                "parameters", tool.parameters())))
                .toList();
    }

    @Override
    protected LLMResponse parseResponse(Object response) {
        // Parse OpenAI response format
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) response;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");

        @SuppressWarnings("unchecked")
        Map<String, Object> firstChoice = choices.get(0);

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");

        String content = (String) message.getOrDefault("content", "");
        String finishReason = (String) firstChoice.get("finish_reason");

        // Parse tool calls if present
        List<ToolCall> toolCalls = parseToolCalls(message);

        // Parse token usage
        @SuppressWarnings("unchecked")
        Map<String, Object> usage = (Map<String, Object>) responseMap.get("usage");
        TokenUsage tokenUsage = parseTokenUsage(usage);

        if (!toolCalls.isEmpty()) {
            return LLMResponse.withToolCalls(content, toolCalls, tokenUsage);
        } else {
            return LLMResponse.create(content, finishReason, tokenUsage);
        }
    }

    private List<ToolCall> parseToolCalls(Map<String, Object> message) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) message.get("tool_calls");

        if (toolCalls == null) {
            return List.of();
        }

        return toolCalls.stream()
                .map(tc -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> function = (Map<String, Object>) tc.get("function");

                    return ToolCall.create(
                            (String) tc.get("id"),
                            (String) function.get("name"),
                            parseArguments((String) function.get("arguments")));
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseArguments(String argumentsJson) {
        // Parse JSON arguments
        // In production, use proper JSON library
        try {
            return new HashMap<>(); // Placeholder
        } catch (Exception e) {
            LOG.error("Failed to parse arguments: {}", argumentsJson);
            return Map.of();
        }
    }

    private TokenUsage parseTokenUsage(Map<String, Object> usage) {
        int promptTokens = ((Number) usage.get("prompt_tokens")).intValue();
        int completionTokens = ((Number) usage.get("completion_tokens")).intValue();

        return TokenUsage.of(promptTokens, completionTokens);
    }

    private Uni<Object> callOpenAIAPI(Map<String, Object> request) {
        // In production, use proper HTTP client
        // For now, return mock response

        Map<String, Object> mockResponse = Map.of(
                "id", "chatcmpl-123",
                "object", "chat.completion",
                "created", System.currentTimeMillis() / 1000,
                "model", request.get("model"),
                "choices", List.of(
                        Map.of(
                                "index", 0,
                                "message", Map.of(
                                        "role", "assistant",
                                        "content", "This is a mock response from OpenAI"),
                                "finish_reason", "stop")),
                "usage", Map.of(
                        "prompt_tokens", 10,
                        "completion_tokens", 20,
                        "total_tokens", 30));

        return Uni.createFrom().item(mockResponse);
    }
}