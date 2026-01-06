package tech.kayys.wayang.plugin.dto;

import io.smallrye.mutiny.Uni;

public sealed interface AgentPlugin extends Plugin permits PlanningAgent, ExecutionAgent,
        OrchestrationAgent, RAGAgent, AnalyticsAgent {

    AgentType getAgentType();

    Uni<AgentResult> execute(AgentContext context);

    default Uni<Boolean> canHandle(TaskDescription task) {
        return Uni.createFrom().item(getCapabilities().stream()
                .anyMatch(cap -> cap.canHandle(task)));
    }
}