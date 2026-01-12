package tech.kayys.wayang.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.project.domain.WayangProject;
import tech.kayys.wayang.project.dto.CreateProjectRequest;
import tech.kayys.wayang.project.dto.ProjectType;
import tech.kayys.wayang.project.service.ControlPlaneService;
import tech.kayys.wayang.security.service.AuthenticatedUser;
import tech.kayys.wayang.security.service.AuthorizationPolicyEngine;
import tech.kayys.wayang.security.service.IketSecurityService;
import tech.kayys.wayang.websocket.service.WebSocketEventBroadcaster;

@QuarkusTest
public class ProjectResourceIT {

        @InjectMock
        ControlPlaneService controlPlaneService;

        @InjectMock
        IketSecurityService iketSecurity;

        @InjectMock
        AuthorizationPolicyEngine authzEngine;

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
        public void testCreateProjectSuccess() {
                CreateProjectRequest request = new CreateProjectRequest(
                                "tenant-1", "New Project", "Desc", ProjectType.AGENTIC_AI, "user-1", new HashMap<>());

                WayangProject project = new WayangProject();
                project.projectId = UUID.randomUUID();
                project.projectName = "New Project";
                project.tenantId = "tenant-1";

                when(authzEngine.authorize(any(), any(), any())).thenReturn(true);
                when(controlPlaneService.createProject(any())).thenReturn(Uni.createFrom().item(project));
                when(wsEventBroadcaster.broadcastToTenant(any(), any())).thenReturn(Uni.createFrom().voidItem());

                given()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(request)
                                .when()
                                .post("/api/v1/projects")
                                .then()
                                .statusCode(201)
                                .body("projectName", is("New Project"))
                                .body("tenantId", is("tenant-1"));
        }

        @Test
        public void testCreateProjectForbidden() {
                CreateProjectRequest request = new CreateProjectRequest(
                                "tenant-1", "New Project", "Desc", ProjectType.AGENTIC_AI, "user-1", new HashMap<>());

                when(authzEngine.authorize(any(), any(), any())).thenReturn(false);

                given()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(request)
                                .when()
                                .post("/api/v1/projects")
                                .then()
                                .statusCode(403);
        }

        @Test
        public void testGetProjectSuccess() {
                UUID projectId = UUID.randomUUID();
                WayangProject project = new WayangProject();
                project.projectId = projectId;
                project.projectName = "Test Project";
                project.tenantId = "tenant-1";

                when(controlPlaneService.getProject(any(), any())).thenReturn(Uni.createFrom().item(project));
                when(authzEngine.authorizeWithAttributes(any(), any(), any(), any())).thenReturn(true);

                given()
                                .pathParam("projectId", projectId)
                                .when()
                                .get("/api/v1/projects/{projectId}")
                                .then()
                                .statusCode(200)
                                .body("projectName", is("Test Project"));
        }

        @Test
        public void testGetProjectNotFound() {
                UUID projectId = UUID.randomUUID();
                when(controlPlaneService.getProject(any(), any())).thenReturn(Uni.createFrom().nullItem());

                given()
                                .pathParam("projectId", projectId)
                                .when()
                                .get("/api/v1/projects/{projectId}")
                                .then()
                                .statusCode(404);
        }
}
