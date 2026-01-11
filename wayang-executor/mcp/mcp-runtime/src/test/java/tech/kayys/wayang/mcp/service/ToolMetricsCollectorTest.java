package tech.kayys.wayang.mcp.service;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ToolMetricsCollectorTest {

    private ToolMetricsCollector metricsCollector;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        metricsCollector = new ToolMetricsCollector();
    }

    @Test
    void testCollectMetricsReturnsVoid() {
        tech.kayys.wayang.mcp.runtime.ToolExecutionResult result = tech.kayys.wayang.mcp.TestFixtures
                .createToolExecutionResult(true);

        Uni<Void> uni = metricsCollector.collect(
                tech.kayys.wayang.mcp.TestFixtures.TEST_TOOL_ID,
                result);

        assertNotNull(uni);
        assertDoesNotThrow(() -> uni.await().indefinitely());
    }

    @Test
    void testCollectMetricsForSuccessfulExecution() {
        tech.kayys.wayang.mcp.runtime.ToolExecutionResult result = tech.kayys.wayang.mcp.TestFixtures
                .createToolExecutionResult(true);

        Uni<Void> uni = metricsCollector.collect("tool-success", result);

        assertDoesNotThrow(() -> uni.await().indefinitely());
    }

    @Test
    void testCollectMetricsForFailedExecution() {
        tech.kayys.wayang.mcp.runtime.ToolExecutionResult result = tech.kayys.wayang.mcp.TestFixtures
                .createToolExecutionResult(false);

        Uni<Void> uni = metricsCollector.collect("tool-failed", result);

        assertDoesNotThrow(() -> uni.await().indefinitely());
    }

    @Test
    void testCollectMetricsHandlesNullToolId() {
        tech.kayys.wayang.mcp.runtime.ToolExecutionResult result = tech.kayys.wayang.mcp.TestFixtures
                .createToolExecutionResult(true);

        Uni<Void> uni = metricsCollector.collect(null, result);

        assertDoesNotThrow(() -> uni.await().indefinitely());
    }

    @Test
    void testCollectMetricsHandlesNullResult() {
        Uni<Void> uni = metricsCollector.collect(
                tech.kayys.wayang.mcp.TestFixtures.TEST_TOOL_ID,
                null);

        assertDoesNotThrow(() -> uni.await().indefinitely());
    }
}
