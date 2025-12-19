package tech.kayys.wayang.agent.dto;

import java.util.List;
import java.util.Map;

public record OrchestratorExecutionResponse(
        String orchestratorId,
        String executionId,
        String status,
        List<String> subAgentResults,
        Map<String, Object> outputs,
        java.time.LocalDateTime startedAt,
        java.time.LocalDateTime completedAt) {
    
    public static OrchestratorExecutionResponse from(Object run, Object orchestrator) {
        return new OrchestratorExecutionResponse(
            "placeholder-orchestrator-id",
            "placeholder-execution-id",
            "COMPLETED",
            List.of("result1", "result2"),
            Map.of("output", "final-result"),
            java.time.LocalDateTime.now(),
            java.time.LocalDateTime.now()
        );
    }
}