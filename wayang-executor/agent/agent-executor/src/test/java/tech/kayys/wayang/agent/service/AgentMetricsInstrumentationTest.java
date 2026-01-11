package tech.kayys.wayang.agent.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class AgentMetricsInstrumentationTest {

    @Inject
    AgentMetricsInstrumentation instrumentation;

    @Inject
    MeterRegistry meterRegistry;

    @Test
    void testRecordMetrics() {
        instrumentation.recordExecutionStart("node-1", "tenant-1");
        instrumentation.recordExecutionComplete("node-1", "tenant-1", Duration.ofMillis(100), true);
        instrumentation.recordTokenUsage("openai", "gpt-4", 10, 20, 30);
        instrumentation.recordLLMCall("openai", "gpt-4", Duration.ofMillis(500));
        instrumentation.recordToolCall("weather", true, Duration.ofMillis(200));

        assertNotNull(meterRegistry.find("agent.executions.total").counter());
        assertTrue(meterRegistry.find("agent.executions.total").counter().count() >= 1);
        assertNotNull(meterRegistry.find("agent.tokens.used.total").counter());
        assertTrue(meterRegistry.find("agent.tokens.used.total").counter().count() >= 30);
    }
}
