package tech.kayys.wayang.sdk.dto.agent;

import java.util.Map;
import java.time.Instant;

public record AgentInfoResponse(
    String agentId,
    String name,
    String status,
    Map<String, Object> capabilities,
    Instant lastSeenAt
) {}
