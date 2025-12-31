package tech.kayys.wayang.workflow.security;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.workflow.domain.WorkflowRun;
import tech.kayys.wayang.workflow.api.model.RunStatus;
import tech.kayys.wayang.workflow.security.model.ExecutionContext;
import tech.kayys.wayang.workflow.security.model.Initiator;
import tech.kayys.wayang.workflow.security.model.Initiator.InitiatorType;
import tech.kayys.wayang.workflow.security.model.Permission;
import tech.kayys.wayang.workflow.security.service.ExecutionContextService;
import io.smallrye.jwt.auth.principal.JWTParser;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.mockito.Mockito;
import java.util.Optional;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.Map;

@QuarkusTest
public class SecurityComponentsTest {

        @Inject
        ExecutionContextService contextService;

        @InjectMock
        JWTParser jwtParser;

        @Test
        public void testExecutionContextCreation() {
                WorkflowRun run = WorkflowRun.builder()
                                .runId("run-1")
                                .workflowId("wf-1")
                                .tenantId("tenant-1")
                                .status(RunStatus.RUNNING)
                                .createdAt(Instant.now())
                                .build();

                Initiator initiator = Initiator.builder()
                                .type(InitiatorType.SYSTEM)
                                .userId("system-scheduler")
                                .roles(Set.of("admin"))
                                .build();

                ExecutionContext context = contextService.createContext(run, initiator);

                Assertions.assertNotNull(context);
                Assertions.assertEquals("run-1", context.runId());
                Assertions.assertEquals("tenant-1", context.tenantId());
                Assertions.assertEquals(InitiatorType.SYSTEM, context.initiator().type());
                Assertions.assertFalse(context.isExpired());

                // Check permissions
                Permission required = Permission.builder()
                                .resource("workflow")
                                .action("execute")
                                .target("wf-1")
                                .build();

                Assertions.assertTrue(context.hasPermission(required));
        }

        @Test
        public void testTokenSigningAndValidation() throws Exception {
                WorkflowRun run = WorkflowRun.builder()
                                .runId("run-2")
                                .workflowId("wf-2")
                                .tenantId("tenant-2")
                                .status(RunStatus.RUNNING)
                                .createdAt(Instant.now())
                                .build();

                Initiator initiator = Initiator.builder()
                                .type(InitiatorType.USER)
                                .userId("alice")
                                .roles(Collections.emptySet())
                                .build();

                ExecutionContext context = contextService.createContext(run, initiator);
                String token = contextService.signContext(context);

                Assertions.assertNotNull(token);
                Assertions.assertFalse(token.isEmpty());

                // Mock JWTParser behavior for validation
                JsonWebToken mockToken = Mockito.mock(JsonWebToken.class);

                Mockito.when(mockToken.getName()).thenReturn("alice");
                Mockito.when(mockToken.getClaim("runId")).thenReturn("run-2");
                Mockito.when(mockToken.getClaim("workflowId")).thenReturn("wf-2");
                Mockito.when(mockToken.getClaim("tenantId")).thenReturn("tenant-2");
                Mockito.when(mockToken.getClaim("environment")).thenReturn("production"); // Service sets default
                                                                                          // "production"
                Mockito.when(mockToken.getClaim("initiatorType")).thenReturn("USER");
                Mockito.when(mockToken.getGroups()).thenReturn(Collections.emptySet());

                // Mock permissions claim
                Set<String> permStrings = context.permissions().stream().map(Permission::toString)
                                .collect(java.util.stream.Collectors.toSet());
                Mockito.when(mockToken.claim("permissions")).thenReturn(Optional.of(permStrings));

                // Mock Expiration (Future) and IssuedAt (Now)
                Mockito.when(mockToken.getIssuedAtTime()).thenReturn(Instant.now().getEpochSecond());
                Mockito.when(mockToken.getExpirationTime())
                                .thenReturn(Instant.now().plusSeconds(3600).getEpochSecond());

                // Mock Parser
                Mockito.when(jwtParser.parse(Mockito.anyString())).thenReturn(mockToken);

                // Validate token
                ExecutionContext validated = null;
                try {
                        validated = contextService.validateContext(token).await().indefinitely();
                } catch (Exception e) {
                        throw e;
                }

                Assertions.assertNotNull(validated);
                Assertions.assertEquals(context.runId(), validated.runId());
                Assertions.assertEquals(context.tenantId(), validated.tenantId());
        }
}
