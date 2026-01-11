package tech.kayys.wayang.agent.model;

import java.util.List;
import java.util.Map;

/**
 * Final result of agent execution
 */
public record AgentExecutionResult(
        LLMResponse finalResponse,
        List<Message> messages,
        int iterations,
        boolean maxIterationsReached,
        Map<String, Object> metadata) {

    public String content() {
        return finalResponse != null ? finalResponse.content() : null;
    }

    public AgentExecutionResult {
        messages = List.copyOf(messages != null ? messages : List.of());
        metadata = Map.copyOf(metadata != null ? metadata : Map.of());
    }

    public static AgentExecutionResult completed(
            LLMResponse finalResponse,
            List<Message> messages,
            int iterations) {
        return new AgentExecutionResult(
                finalResponse,
                messages,
                iterations,
                false,
                Map.of());
    }

    public static AgentExecutionResult maxIterationsReached(
            List<Message> messages,
            int iterations) {
        return new AgentExecutionResult(
                null,
                messages,
                iterations,
                true,
                Map.of("reason", "max_iterations_reached"));
    }

    public boolean hadToolCalls() {
        return messages.stream().anyMatch(Message::hasToolCalls);
    }

    public int countToolCalls() {
        return messages.stream()
                .filter(Message::hasToolCalls)
                .mapToInt(m -> m.toolCalls().size())
                .sum();
    }
}
