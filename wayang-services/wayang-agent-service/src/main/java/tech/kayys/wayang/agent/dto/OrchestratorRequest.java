package tech.kayys.wayang.agent.dto;

import java.util.List;

public record OrchestratorRequest(
        String name,
        String tenantId,
        String orchestratorAgentId,
        OrchestrationStrategy strategy,
        List<SubAgentConfig> subAgents,
        AggregationStrategy aggregationStrategy) {
}
