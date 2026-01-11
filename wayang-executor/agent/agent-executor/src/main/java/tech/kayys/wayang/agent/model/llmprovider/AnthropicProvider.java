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
public class AnthropicProvider extends AbstractLLMProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AnthropicProvider.class);

    private final String apiKey;

    public AnthropicProvider() {
        super("anthropic", Map.of(
                "baseUrl", "https://api.anthropic.com/v1",
                "timeout", 60));

        this.apiKey = System.getenv("ANTHROPIC_API_KEY");
    }

    @Override
    public Uni<LLMResponse> complete(LLMRequest request) {
        LOG.info("Anthropic completion: model={}, messages={}",
                request.model(), request.messages().size());
        validateRequest(request);

        LOG.debug("Anthropic completion: model={}, messages={}",
                request.model(), request.messages().size());

        // Similar to OpenAI but with Anthropic API format
        return Uni.createFrom().deferred(() -> {
            Map<String, Object> apiRequest = buildApiRequest(request);
            return callAnthropicAPI(apiRequest)
                    .map(this::parseResponse);
        });
    }

    @Override
    public io.smallrye.mutiny.Multi<String> stream(LLMRequest request) {
        return io.smallrye.mutiny.Multi.createFrom().items(
                "Streaming not yet implemented for Anthropic");
    }

    @Override
    public boolean supportsFunctionCalling() {
        return true; // Claude supports tool use
    }

    @Override
    public List<String> supportedModels() {
        return List.of(
                "claude-3-opus-20240229",
                "claude-3-sonnet-20240229",
                "claude-3-haiku-20240307",
                "claude-2.1",
                "claude-2.0");
    }

    private Map<String, Object> buildApiRequest(LLMRequest request) {
        // Anthropic API format
        Map<String, Object> apiRequest = new HashMap<>();

        apiRequest.put("model", request.model());
        apiRequest.put("messages", convertMessages(request.messages()));
        apiRequest.put("max_tokens", request.maxTokens());

        if (!request.tools().isEmpty()) {
            apiRequest.put("tools", convertTools(request.tools()));
        }

        return apiRequest;
    }

    @Override
    protected Object convertMessages(List<Message> messages) {
        // Convert to Anthropic format
        return messages.stream()
                .filter(msg -> !msg.isSystem()) // System messages handled separately
                .map(msg -> Map.of(
                        "role", msg.role(),
                        "content", msg.content()))
                .toList();
    }

    @Override
    protected Object convertTools(List<ToolDefinition> tools) {
        // Anthropic tool format
        return tools.stream()
                .map(tool -> Map.of(
                        "name", tool.name(),
                        "description", tool.description(),
                        "input_schema", tool.parameters()))
                .toList();
    }

    @Override
    protected LLMResponse parseResponse(Object response) {
        // Parse Anthropic response
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = (Map<String, Object>) response;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) responseMap.get("content");

        String textContent = extractTextContent(content);
        List<ToolCall> toolCalls = extractToolUse(content);

        @SuppressWarnings("unchecked")
        Map<String, Object> usage = (Map<String, Object>) responseMap.get("usage");
        TokenUsage tokenUsage = TokenUsage.of(
                ((Number) usage.get("input_tokens")).intValue(),
                ((Number) usage.get("output_tokens")).intValue());

        String finishReason = (String) responseMap.get("stop_reason");

        if (!toolCalls.isEmpty()) {
            return LLMResponse.withToolCalls(textContent, toolCalls, tokenUsage);
        } else {
            return LLMResponse.create(textContent, finishReason, tokenUsage);
        }
    }

    private String extractTextContent(List<Map<String, Object>> content) {
        return content.stream()
                .filter(block -> "text".equals(block.get("type")))
                .map(block -> (String) block.get("text"))
                .findFirst()
                .orElse("");
    }

    private List<ToolCall> extractToolUse(List<Map<String, Object>> content) {
        return content.stream()
                .filter(block -> "tool_use".equals(block.get("type")))
                .map(block -> ToolCall.create(
                        (String) block.get("id"),
                        (String) block.get("name"),
                        (Map<String, Object>) block.get("input")))
                .toList();
    }

    private Uni<Object> callAnthropicAPI(Map<String, Object> request) {
        // Mock response
        Map<String, Object> mockResponse = Map.of(
                "id", "msg_123",
                "type", "message",
                "role", "assistant",
                "content", List.of(
                        Map.of(
                                "type", "text",
                                "text", "This is a mock response from Claude")),
                "model", request.get("model"),
                "stop_reason", "end_turn",
                "usage", Map.of(
                        "input_tokens", 10,
                        "output_tokens", 20));

        return Uni.createFrom().item(mockResponse);
    }
}