package tech.kayys.wayang.sdk.dto.agent;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;




/**
 * Agent execution response
 */
public record AgentExecutionResponse(
    String executionId,
    Map<String, Object> outputs,
    AgentMetrics metrics,
    List<String> toolsUsed
) {}
