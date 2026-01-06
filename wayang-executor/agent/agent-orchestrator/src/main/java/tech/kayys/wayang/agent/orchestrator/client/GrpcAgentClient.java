package tech.kayys.wayang.agent.orchestrator.client;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.dto.AgentExecutionRequest;
import tech.kayys.wayang.agent.dto.AgentExecutionResult;
import tech.kayys.wayang.agent.dto.AgentRegistration;

@ApplicationScoped
public class GrpcAgentClient {
    public Uni<AgentExecutionResult> execute(
            AgentRegistration agent,
            AgentExecutionRequest request) {
        // gRPC implementation
        return Uni.createFrom().nullItem();
    }
}
