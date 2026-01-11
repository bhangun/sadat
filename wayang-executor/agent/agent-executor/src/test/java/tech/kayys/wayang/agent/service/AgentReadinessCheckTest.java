package tech.kayys.wayang.agent.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.repository.AgentConfigurationRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@QuarkusTest
public class AgentReadinessCheckTest {

    @Inject
    AgentReadinessCheck readinessCheck;

    @InjectMock
    AgentConfigurationRepository configRepo;

    @Test
    void testReadinessCheckUp() {
        when(configRepo.count()).thenReturn(Uni.createFrom().item(10L));

        HealthCheckResponse response = readinessCheck.call();

        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        assertEquals("agent-system-ready", response.getName());
        assertEquals("connected", response.getData().get().get("database"));
    }

    @Test
    void testReadinessCheckDown() {
        when(configRepo.count()).thenReturn(Uni.createFrom().failure(new RuntimeException("Connection timeout")));

        HealthCheckResponse response = readinessCheck.call();

        assertEquals(HealthCheckResponse.Status.DOWN, response.getStatus());
        assertEquals("disconnected", response.getData().get().get("database"));
    }
}
