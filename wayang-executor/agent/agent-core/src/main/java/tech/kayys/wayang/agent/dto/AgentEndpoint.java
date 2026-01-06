package tech.kayys.wayang.agent.dto;

import java.util.Map;

/**
 * Agent Endpoint
 */
public record AgentEndpoint(
    EndpointType type,
    String address,
    Map<String, String> connectionParams
) {}
