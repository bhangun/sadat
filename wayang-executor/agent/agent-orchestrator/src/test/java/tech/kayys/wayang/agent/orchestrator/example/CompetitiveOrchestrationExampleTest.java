package tech.kayys.wayang.agent.orchestrator.example;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class CompetitiveOrchestrationExampleTest {

    @Inject
    CompetitiveOrchestrationExample example;

    @Test
    public void testCompetitiveSolution() {
        // Given
        String challenge = "Solve a competitive programming problem";

        // When
        Map<String, Object> result = example.competitiveSolution(challenge).await().indefinitely();

        // Then
        assertThat(result).isNotNull();
    }
}