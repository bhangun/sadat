package tech.kayys.wayang.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.panache.mock.PanacheMock;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import tech.kayys.wayang.project.domain.IntegrationPattern;
import tech.kayys.wayang.project.dto.EIPPatternType;
import tech.kayys.wayang.project.dto.IntegrationExecutionResult;

@QuarkusTest
public class IntegrationPatternExecutorTest {

    @Inject
    IntegrationPatternExecutor executor;

    @InjectMock
    TransformationEngine transformationEngine;

    @InjectMock
    EndpointInvoker endpointInvoker;

    @Test
    @RunOnVertxContext
    public Uni<Void> testExecuteFailsWhenPatternNotFound() {
        PanacheMock.mock(IntegrationPattern.class);
        when(IntegrationPattern.findById(any())).thenReturn(Uni.createFrom().nullItem());

        return executor.execute(UUID.randomUUID(), new HashMap<>())
                .onItem().failWith(() -> new AssertionError("Expected failure"))
                .onFailure(IllegalArgumentException.class).recoverWithItem(err -> {
                    assertTrue(err.getMessage().contains("Pattern not found"));
                    return (IntegrationExecutionResult) null;
                })
                .replaceWithVoid();
    }
}
