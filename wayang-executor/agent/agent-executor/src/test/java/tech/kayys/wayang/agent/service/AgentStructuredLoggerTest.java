package tech.kayys.wayang.agent.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AgentStructuredLoggerTest {

    @Inject
    AgentStructuredLogger logger;

    @Test
    void testLogging() {
        // These methods primarily log, so we just verify they don't throw exceptions
        logger.logExecutionStart("run-1", "node-1", "tenant-1", "session-1");
        logger.logExecutionComplete("run-1", "node-1", true, 2, 500, 100);
        logger.logExecutionComplete("run-2", "node-2", false, 1, 1000, 50);
        logger.logLLMCall("openai", "gpt-4", 2, 10, 20);
        logger.logToolExecution("weather", true, 200, null);
        logger.logToolExecution("error-tool", false, 100, "Tool error");
        logger.logMemoryOperation("load", "session-1", 5);
    }
}
