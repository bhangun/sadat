package tech.kayys.wayang.agent.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.model.AgentConfiguration;
import tech.kayys.wayang.agent.repository.ConfigurationRepository;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@QuarkusTest
public class AgentConfigurationServiceTest {

    @Inject
    AgentConfigurationService service;

    @InjectMock
    ConfigurationRepository repository;

    @Test
    void testLoadConfigurationCacheHit() {
        String agentId = "test-agent";
        String tenantId = "test-tenant";
        AgentConfiguration config = AgentConfiguration.builder()
                .agentId(agentId)
                .tenantId(tenantId)
                .build();

        // First call to populate cache
        when(repository.findByAgentId(agentId, tenantId)).thenReturn(Uni.createFrom().item(config));

        service.loadConfiguration(agentId, tenantId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(config);

        // Second call should hit cache
        service.loadConfiguration(agentId, tenantId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(config);

        verify(repository, times(1)).findByAgentId(agentId, tenantId);
    }

    @Test
    void testLoadConfigurationCacheMiss() {
        String agentId = "new-agent";
        String tenantId = "test-tenant";
        AgentConfiguration config = AgentConfiguration.builder()
                .agentId(agentId)
                .tenantId(tenantId)
                .build();

        when(repository.findByAgentId(agentId, tenantId)).thenReturn(Uni.createFrom().item(config));

        service.loadConfiguration(agentId, tenantId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(config);

        verify(repository).findByAgentId(agentId, tenantId);
    }

    @Test
    void testSaveConfiguration() {
        AgentConfiguration config = AgentConfiguration.builder()
                .agentId("save-agent")
                .tenantId("test-tenant")
                .build();

        when(repository.save(config)).thenReturn(Uni.createFrom().voidItem());

        service.saveConfiguration(config)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertCompleted();

        verify(repository).save(config);

        // Verify it's in cache
        service.loadConfiguration(config.agentId(), config.tenantId())
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem(config);

        verify(repository, times(0)).findByAgentId(anyString(), anyString());
    }

    @Test
    void testDeleteConfiguration() {
        String agentId = "del-agent";
        String tenantId = "test-tenant";

        when(repository.delete(agentId, tenantId)).thenReturn(Uni.createFrom().voidItem());

        service.deleteConfiguration(agentId, tenantId)
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertCompleted();

        verify(repository).delete(agentId, tenantId);
    }
}
