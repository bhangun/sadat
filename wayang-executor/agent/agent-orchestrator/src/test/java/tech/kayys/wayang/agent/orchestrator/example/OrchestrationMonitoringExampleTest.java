package tech.kayys.wayang.agent.orchestrator.example;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class OrchestrationMonitoringExampleTest {

    @Inject
    OrchestrationMonitoringExample example;

    @Test
    public void testMonitoredExecution() {
        // Given
        String task = "Execute a task with monitoring";

        // When
        Map<String, Object> result = example.monitoredExecution(task).await().indefinitely();

        // Then
        assertThat(result).isNotNull();
    }
}