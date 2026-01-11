package tech.kayys.wayang.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import tech.kayys.wayang.project.dto.AgentTask;

@QuarkusTest
public class AgentOrchestratorTest {

    @Inject
    AgentOrchestrator agentOrchestrator;

    @InjectMock
    LLMProviderFactory llmFactory;

    @InjectMock
    AgentMemoryManager memoryManager;

    @InjectMock
    AgentGuardrailEngine guardrailEngine;

    @InjectMock
    LLMProvider llmProvider;

    @Test
    @RunOnVertxContext
    public Uni<Void> testExecuteTaskFailsWhenNotActive() {
        AgentTask task = new AgentTask("task-1", "Instruction", new HashMap<>(), List.of());

        return agentOrchestrator.executeTask(UUID.randomUUID(), task)
                .onItem().failWith(() -> new AssertionError("Expected failure"))
                .onFailure(IllegalStateException.class).recoverWithItem(err -> {
                    assertEquals("Agent not active", err.getMessage());
                    return null;
                })
                .replaceWithVoid();
    }

    // Activating and then executing task is more complex because of internal state
    // (ConcurrentHashMap)
    // and Panache static methods in activateAgent.
    // In a real environment, activateAgent would be called first.
}
