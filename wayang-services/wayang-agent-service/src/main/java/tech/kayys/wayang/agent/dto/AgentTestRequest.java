package tech.kayys.wayang.agent.dto;

import java.util.Map;

public record AgentTestRequest(
        Map<String, Object> inputs,
        Map<String, Object> context) {
}