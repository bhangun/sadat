package tech.kayys.wayang.agent.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.dto.PerformanceStats;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class AgentPerformanceMonitorTest {

    @Inject
    AgentPerformanceMonitor monitor;

    @Test
    void testCheckPerformance() {
        // This method primarily logs warnings, so we just verify it doesn't throw
        // exceptions
        monitor.checkPerformance("run-1", 1000, 1, 100);
        monitor.checkPerformance("run-slow", 40000, 1, 100); // Trigger slow warning
        monitor.checkPerformance("run-high-tokens", 1000, 1, 5000); // Trigger high tokens warning
        monitor.checkPerformance("run-many-iters", 1000, 10, 100); // Trigger iterations warning
    }

    @Test
    void testGetStats() {
        PerformanceStats stats = monitor.getStats();
        assertNotNull(stats);
    }
}
