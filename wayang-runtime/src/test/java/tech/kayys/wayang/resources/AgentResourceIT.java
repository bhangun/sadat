package tech.kayys.wayang.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.project.domain.AIAgent;
import tech.kayys.wayang.project.dto.AgentType;
import tech.kayys.wayang.project.dto.CreateAgentRequest;
import tech.kayys.wayang.project.service.ControlPlaneService;
import tech.kayys.wayang.security.service.AuthenticatedUser;
import tech.kayys.wayang.security.service.IketSecurityService;
import tech.kayys.wayang.websocket.service.WebSocketEventBroadcaster;
import tech.kayys.wayang.guardrails.service.GuardrailEngine;

@QuarkusTest
public class AgentResourceIT {

    @InjectMock
    ControlPlaneService controlPlaneService;

    @InjectMock
    IketSecurityService iketSecurity;

    @InjectMock
    GuardrailEngine guardrailEngine;

    @InjectMock
    WebSocketEventBroadcaster wsEventBroadcaster;

    private AuthenticatedUser testUser;

    @BeforeEach
    public void setup() {
        testUser = new AuthenticatedUser(
                "user-1", "Test User", "test@example.com", "tenant-1",
                Set.of("admin"), Set.of(), new HashMap<>());
        when(iketSecurity.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    public void testCreateAgentSuccess() {
        CreateAgentRequest request = new CreateAgentRequest(
                "Test Agent", "Desc", AgentType.CONVERSATIONAL, null, List.of(), List.of(), null, List.of());

        AIAgent agent = new AIAgent();
        agent.agentId = UUID.randomUUID();
        agent.agentName = "Test Agent";

        when(controlPlaneService.createAgent(any(), any())).thenReturn(Uni.createFrom().item(agent));
        when(wsEventBroadcaster.broadcastToTenant(any(), any())).thenReturn(Uni.createFrom().voidItem());

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .queryParam("projectId", UUID.randomUUID())
                .body(request)
                .when()
                .post("/api/v1/agents")
                .then()
                .statusCode(201)
                .body("agentName", is("Test Agent"));
    }
}
