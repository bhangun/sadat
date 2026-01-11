package tech.kayys.wayang.agent.service;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
public class ToolExecutionSandboxTest {

    @Inject
    ToolExecutionSandbox sandbox;

    @InjectMock
    SecurityContext securityContext;

    @Test
    void testExecuteSecurelySuccess() {
        when(securityContext.hasPermission(anyString())).thenReturn(true);

        sandbox.executeSecurely("test-tool", () -> Uni.createFrom().item("tool-result"))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitItem()
                .assertItem("tool-result");
    }

    @Test
    void testExecuteSecurelyDenied() {
        when(securityContext.hasPermission(anyString())).thenReturn(false);

        sandbox.executeSecurely("denied-tool", () -> Uni.createFrom().item("should-not-run"))
                .subscribe().withSubscriber(UniAssertSubscriber.create())
                .awaitFailure();
    }
}
