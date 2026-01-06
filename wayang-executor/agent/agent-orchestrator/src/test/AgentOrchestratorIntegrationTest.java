


/**
 * Integration test demonstrating full orchestrator lifecycle
 */
public class AgentOrchestratorIntegrationTest {
    
    @Inject
    AgentOrchestratorExecutor orchestrator;
    
    @Inject
    AgentRegistry registry;
    
    public void testCompleteOrchestrationFlow() {
        // 1. Register agents
        registry.registerAgent(
            "test-agent",
            "Test Agent",
            new CommonAgent("test", Set.of("testing")),
            Set.of(AgentCapability.REASONING),
            new AgentEndpoint(EndpointType.INTERNAL, "internal://test", Map.of()),
            "test-tenant"
        ).await().indefinitely();
        
        // 2. Create execution request
        AgentExecutionRequest request = AgentExecutionRequest.builder()
            .taskDescription("Test orchestration flow")
            .context("testMode", true)
            .build();
        
        // 3. Execute
        NodeExecutionTask task = new NodeExecutionTask(
            tech.kayys.silat.core.domain.WorkflowRunId.generate(),
            tech.kayys.silat.core.domain.NodeId.of("test-node"),
            1,
            new tech.kayys.silat.core.domain.ExecutionToken(
                UUID.randomUUID().toString(),
                tech.kayys.silat.core.domain.WorkflowRunId.generate(),
                tech.kayys.silat.core.domain.NodeId.of("test-node"),
                1,
                Instant.now().plusSeconds(3600)
            ),
            new HashMap<>(request.context())
        );
        
        tech.kayys.silat.executor.NodeExecutionResult result = 
            orchestrator.execute(task).await().indefinitely();
        
        // 4. Verify results
        assert result.status().equals(
            tech.kayys.silat.core.domain.NodeExecutionStatus.COMPLETED);
        
        // 5. Cleanup
        registry.deregisterAgent("test-agent").await().indefinitely();
    }
}

