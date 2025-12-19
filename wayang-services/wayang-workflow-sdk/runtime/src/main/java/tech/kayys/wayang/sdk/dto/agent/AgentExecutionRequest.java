package tech.kayys.wayang.sdk.dto.agent;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;




/**
 * Agent execution request
 */
public record AgentExecutionRequest(
    Map<String, Object> inputs,
    Map<String, Object> context,
    ExecutionMode mode,
    int maxTokens,
    double temperature
) {
    public enum ExecutionMode {
        SYNC, ASYNC, STREAM
    }

    public static AgentExecutionRequest simple(Map<String, Object> inputs) {
        return new AgentExecutionRequest(inputs, Map.of(), ExecutionMode.SYNC, 1000, 0.7);
    }
}
