package tech.kayys.wayang.agent.orchestrator.example;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class HierarchicalOrchestrationExampleTest {

    @Inject
    HierarchicalOrchestrationExample example;

    @Test
    public void testComplexProjectExecution() {
        // Given
        String projectDescription = "Execute a complex software development project";

        // When
        Map<String, Object> result = example.complexProjectExecution(projectDescription).await().indefinitely();

        // Then
        assertThat(result).isNotNull();
    }
}