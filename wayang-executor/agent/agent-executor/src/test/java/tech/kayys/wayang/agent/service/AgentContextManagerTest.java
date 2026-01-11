package tech.kayys.wayang.agent.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.model.AgentConfiguration;
import tech.kayys.wayang.agent.model.AgentContext;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class AgentContextManagerTest {

    @Inject
    AgentContextManager manager;

    @Test
    void testCreateAndGetContext() {
        String sessionId = "session-1";
        String runId = "run-1";
        String nodeId = "node-1";
        String tenantId = "tenant-1";
        AgentConfiguration config = AgentConfiguration.builder().build();
        Map<String, Object> taskContext = Map.of("task", "test");

        AgentContext context = manager.createContext(sessionId, runId, nodeId, tenantId, config, taskContext);

        assertNotNull(context);
        assertEquals(sessionId, context.sessionId());
        assertEquals(runId, context.runId());

        Optional<AgentContext> retrieved = manager.getContext(sessionId, runId);
        assertTrue(retrieved.isPresent());
        assertEquals(context, retrieved.get());

        assertEquals(1, manager.getActiveContextCount());
        assertFalse(manager.getActiveContexts().isEmpty());
    }

    @Test
    void testRemoveContext() {
        String sessionId = "session-2";
        String runId = "run-2";

        manager.createContext(sessionId, runId, "node", "tenant", AgentConfiguration.builder().build(), Map.of());
        assertTrue(manager.getContext(sessionId, runId).isPresent());

        manager.removeContext(sessionId, runId);
        assertFalse(manager.getContext(sessionId, runId).isPresent());
        assertEquals(0, manager.getActiveContextCount());
    }
}
