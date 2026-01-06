package tech.kayys.wayang.agent.orchestrator.example;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class CoderAgentExampleTest {

    @Inject
    CoderAgentExample example;

    @Test
    public void testGenerateAndTestCode() {
        // Given
        String specification = "Create a REST API endpoint for user management";

        // When
        String result = example.generateAndTestCode(specification).await().indefinitely();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isNotEmpty();
    }
}