package tech.kayys.wayang.sdk.dto.agent;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;



/**
 * Agent metrics
 */
public record AgentMetrics(
    long durationMs,
    int tokensUsed,
    int promptTokens,
    int completionTokens,
    double cost
) {}
