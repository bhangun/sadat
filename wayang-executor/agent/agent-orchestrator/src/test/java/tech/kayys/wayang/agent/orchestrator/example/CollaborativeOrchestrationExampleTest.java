package tech.kayys.wayang.agent.orchestrator.example;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class CollaborativeOrchestrationExampleTest {

    @Inject
    CollaborativeOrchestrationExample example;

    @Test
    public void testCollaborativeProblemSolving() {
        // Given
        String problemDescription = "Solve a complex optimization problem";

        // When
        Map<String, Object> result = example.collaborativeProblemSolving(problemDescription).await().indefinitely();

        // Then
        assertThat(result).isNotNull();
    }
}