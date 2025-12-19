package tech.kayys.wayang.sdk.dto.agent;

import java.time.Instant;
import java.util.Map;

public record AgentExecutionRecord(
    String executionId,
    String agentId,
    String status,
    Instant startedAt,
    Instant completedAt,
    Map<String, Object> output
) {}
