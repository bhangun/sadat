package tech.kayys.wayang.agent.orchestrator.example;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class AdaptiveReplanningExampleTest {

    @Inject
    AdaptiveReplanningExample example;

    @Test
    public void testResilientExecution() {
        // Given
        String task = "Test task for adaptive replanning";

        // When
        Map<String, Object> result = example.resilientExecution(task).await().indefinitely();

        // Then
        assertThat(result).isNotNull();
        // Add more specific assertions based on expected output
    }
}