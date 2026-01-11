package tech.kayys.wayang.agent.dto;

import java.util.List;

public record AgentPrincipal(
                String userId,
                String tenantId,
                List<String> roles,
                List<String> permissions) {
}
