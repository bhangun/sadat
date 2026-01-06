package tech.kayys.wayang.agent.orchestrator.example;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class SimpleOrchestrationExampleTest {

    @Inject
    SimpleOrchestrationExample example;

    @Test
    public void testAnalyzeCustomerData() {
        // Given
        String customerId = "test-customer-123";

        // When
        Map<String, Object> result = example.analyzeCustomerData(customerId).await().indefinitely();

        // Then
        assertThat(result).isNotNull();
        // Add more specific assertions based on expected output
    }
}