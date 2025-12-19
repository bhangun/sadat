package tech.kayys.wayang.agent.service;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.agent.dto.AgentTestRequest;
import tech.kayys.wayang.agent.dto.BenchmarkRequest;
import tech.kayys.wayang.agent.dto.AgentTestResult;
import tech.kayys.wayang.agent.dto.AgentBenchmarkResult;

@ApplicationScoped
public class AgentTestingService {

    public Uni<AgentTestResult> runTest(String agentId, AgentTestRequest request) {
        Log.infof("Running test for agent: %s", agentId);

        // Simulate test execution
        AgentTestResult result = new AgentTestResult(
            agentId,
            "COMPLETED",
            System.currentTimeMillis(),
            System.currentTimeMillis() + 100, // simulate 100ms execution
            request.inputs(),
            java.util.Map.of("result", "Test completed successfully"),
            java.util.Collections.emptyList(),
            null
        );

        return Uni.createFrom().item(result);
    }

    public Uni<AgentBenchmarkResult> runBenchmark(String agentId, BenchmarkRequest request) {
        Log.infof("Running benchmark for agent: %s", agentId);

        // Simulate benchmark execution
        AgentBenchmarkResult result = new AgentBenchmarkResult(
            agentId,
            request.iterations(),
            1000, // avg response time ms
            95.5, // success rate
            450.0, // tokens per second
            System.currentTimeMillis(),
            System.currentTimeMillis() + 5000 // 5 second test
        );

        return Uni.createFrom().item(result);
    }
}