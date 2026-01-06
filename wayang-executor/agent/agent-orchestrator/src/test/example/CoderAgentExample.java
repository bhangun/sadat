package tech.kayys.wayang.agent.orchestrator.example;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.silat.execution.NodeExecutionTask;
import tech.kayys.wayang.agent.AgentCapability;
import tech.kayys.wayang.agent.CodeCapability;
import tech.kayys.wayang.agent.CoderAgent;
import tech.kayys.wayang.agent.dto.AgentEndpoint;
import tech.kayys.wayang.agent.dto.AgentExecutionRequest;
import tech.kayys.wayang.agent.dto.AgentRegistration;
import tech.kayys.wayang.agent.dto.EndpointType;
import tech.kayys.wayang.agent.orchestrator.service.AgentOrchestratorExecutor;
import tech.kayys.wayang.agent.orchestrator.service.AgentRegistry;

/**
 * Example 3: Code Generation with Coder Agent
 */
@ApplicationScoped
public class CoderAgentExample {
    
    private static final Logger LOG = LoggerFactory.getLogger(CoderAgentExample.class);
    
    @Inject
    AgentOrchestratorExecutor orchestrator;
    
    @Inject
    AgentRegistry agentRegistry;
    
    /**
     * Example: Generate and test code using coder agent
     */
    public Uni<String> generateAndTestCode(String specification) {
        LOG.info("Generating code from specification");
        
        // Register coder agent
        registerCoderAgent().await().indefinitely();
        
        // Create code generation request
        AgentExecutionRequest request = AgentExecutionRequest.builder()
            .taskDescription("Generate production-ready code: " + specification)
            .context("language", "java")
            .context("framework", "quarkus")
            .context("includeTests", true)
            .requiredCapability("CODE_GENERATION")
            .requiredCapability("CODE_ANALYSIS")
            .build();
        
        NodeExecutionTask task = createCodeGenTask(request);
        
        return orchestrator.execute(task)
            .map(result -> {
                if (result.status().equals(tech.kayys.silat.core.domain.NodeExecutionStatus.COMPLETED)) {
                    Map<String, Object> output = (Map<String, Object>) result.output();
                    return (String) output.get("generatedCode");
                }
                throw new RuntimeException("Code generation failed");
            });
    }
    
    private Uni<AgentRegistration> registerCoderAgent() {
        CoderAgent coderType = new CoderAgent(
            Set.of("java", "python", "typescript"),
            Set.of(
                CodeCapability.CODE_GENERATION,
                CodeCapability.CODE_REVIEW,
                CodeCapability.TEST_GENERATION
            ),
            "jdk-17"
        );
        
        return agentRegistry.registerAgent(
            "coder-agent-1",
            "Senior Coder Agent",
            coderType,
            Set.of(
                AgentCapability.CODE_GENERATION,
                AgentCapability.CODE_ANALYSIS,
                AgentCapability.REASONING
            ),
            new AgentEndpoint(EndpointType.GRPC, "localhost:9095", Map.of()),
            "demo-tenant"
        );
    }
    
    private NodeExecutionTask createCodeGenTask(AgentExecutionRequest request) {
        return null;
    }
}
