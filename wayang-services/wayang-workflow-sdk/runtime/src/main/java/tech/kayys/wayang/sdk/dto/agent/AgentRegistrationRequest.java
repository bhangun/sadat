package tech.kayys.wayang.sdk.dto.agent;

import java.util.Map;

public record AgentRegistrationRequest(
    String agentId,
    String name,
    Map<String, Object> capabilities
) {}
