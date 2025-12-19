package tech.kayys.wayang.agent.dto;

import java.util.Map;

public record SubAgentConfig(
                String agentId,
                String name,
                String role,
                Map<String, Object> inputMapping,
                ResourceRequirements resourceRequirements) {
}
