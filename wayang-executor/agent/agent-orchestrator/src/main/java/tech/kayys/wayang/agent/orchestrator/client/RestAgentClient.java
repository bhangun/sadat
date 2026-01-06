package tech.kayys.wayang.agent.orchestrator.client;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.dto.AgentExecutionRequest;
import tech.kayys.wayang.agent.dto.AgentExecutionResult;
import tech.kayys.wayang.agent.dto.AgentRegistration;

@ApplicationScoped
public class RestAgentClient {
    public Uni<AgentExecutionResult> execute(
            AgentRegistration agent,
            AgentExecutionRequest request) {
        // REST implementation
        return Uni.createFrom().nullItem();
    }
}
