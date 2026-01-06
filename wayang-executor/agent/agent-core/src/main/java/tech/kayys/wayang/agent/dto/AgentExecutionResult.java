package tech.kayys.wayang.agent.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Agent Execution Result
 */
public record AgentExecutionResult(
    String requestId,
    String agentId,
    ExecutionStatus status,
    Object output,
    List<String> actionsTaken,
    ExecutionMetrics metrics,
    List<ExecutionError> errors,
    Map<String, Object> metadata,
    Instant completedAt
) {
    
    public boolean isSuccess() {
        return status == ExecutionStatus.SUCCESS;
    }
    
    public boolean isFailure() {
        return status == ExecutionStatus.FAILED;
    }
}
