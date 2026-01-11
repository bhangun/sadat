package tech.kayys.wayang.agent.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.model.TokenUsage;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class AgentMetricsCollectorTest {

    @Inject
    AgentMetricsCollector collector;

    @Test
    void testRecordAndGetNodeMetrics() {
        String nodeId = "node-1";
        collector.recordExecution(nodeId, Duration.ofMillis(100), true);
        collector.recordExecution(nodeId, Duration.ofMillis(200), false);

        AgentMetricsCollector.NodeMetrics metrics = collector.getNodeMetrics(nodeId);
        assertNotNull(metrics);
        assertEquals(2, metrics.getTotalExecutions());
        assertEquals(1, metrics.getSuccessfulExecutions());
        assertEquals(1, metrics.getFailedExecutions());
        assertEquals(50.0, metrics.getSuccessRate());
        assertEquals(150.0, metrics.getAverageDuration());

        Map<String, Object> map = metrics.toMap();
        assertEquals(nodeId, map.get("nodeId"));
    }

    @Test
    void testRecordAndGetProviderMetrics() {
        String provider = "openai";
        String model = "gpt-4";
        TokenUsage usage = TokenUsage.of(100, 50);

        collector.recordTokenUsage(provider, model, usage);

        AgentMetricsCollector.ProviderMetrics metrics = collector.getProviderMetrics(provider, model);
        assertNotNull(metrics);
        assertEquals(1, metrics.getTotalRequests());
        assertEquals(150, metrics.getTotalTokens());
        assertEquals(150, metrics.getAverageTokensPerRequest());

        Map<String, Object> map = metrics.toMap();
        assertEquals(provider, map.get("provider"));
        assertEquals(model, map.get("model"));
    }

    @Test
    void testGetStatistics() {
        collector.recordExecution("node-a", Duration.ofMillis(100), true);
        collector.recordTokenUsage("anthropic", "claude-3", TokenUsage.of(10, 20));

        AgentStatistics stats = collector.getStatistics();
        assertNotNull(stats);
        assertTrue(stats.totalExecutions() >= 1);
        assertTrue(stats.totalTokens() >= 30);
    }
}
