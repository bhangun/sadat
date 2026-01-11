package tech.kayys.wayang.agent.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.model.AgentMemoryManager;
import tech.kayys.wayang.agent.model.ToolRegistry;
import tech.kayys.wayang.agent.model.llmprovider.LLMProviderRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@QuarkusTest
public class AgentHealthCheckTest {

    @Inject
    AgentHealthCheck healthCheck;

    @InjectMock
    LLMProviderRegistry llmRegistry;

    @InjectMock
    ToolRegistry toolRegistry;

    @InjectMock
    AgentMemoryManager memoryManager;

    @Test
    void testHealthCheckUp() {
        when(llmRegistry.getAvailableProviders()).thenReturn(List.of("openai", "anthropic"));

        HealthCheckResponse response = healthCheck.call();

        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        assertEquals("agent-system", response.getName());
        assertEquals(2, response.getData().get().get("llm_providers"));
    }

    @Test
    void testHealthCheckDown() {
        when(llmRegistry.getAvailableProviders()).thenThrow(new RuntimeException("Registry error"));

        HealthCheckResponse response = healthCheck.call();

        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
        assertEquals("Registry error", response.getData().get().get("error"));
    }
}
