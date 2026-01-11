package tech.kayys.wayang.project.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import tech.kayys.wayang.project.domain.WayangProject;
import tech.kayys.wayang.project.dto.CreateProjectRequest;
import tech.kayys.wayang.project.dto.ProjectType;
import tech.kayys.silat.engine.WorkflowRunManager;
import tech.kayys.silat.workflow.WorkflowDefinitionRegistry;

@QuarkusTest
public class ControlPlaneServiceTest {

        @Inject
        ControlPlaneService controlPlaneService;

        @InjectMock
        WorkflowRunManager workflowRunManager;

        @InjectMock
        WorkflowDefinitionRegistry definitionRegistry;

        @InjectMock
        CanvasToWorkflowConverter canvasConverter;

        @InjectMock
        AgentOrchestrator agentOrchestrator;

        @InjectMock
        IntegrationPatternExecutor patternExecutor;

        @Test
        @RunOnVertxContext
        public Uni<Void> testCreateProject() {
                CreateProjectRequest request = new CreateProjectRequest(
                                "tenant-1",
                                "Test Project",
                                "Test Description",
                                ProjectType.AGENTIC_AI,
                                "user-1",
                                new HashMap<>());

                return controlPlaneService.createProject(request)
                                .invoke(project -> {
                                        assertNotNull(project);
                                        assertEquals("tenant-1", project.tenantId);
                                        assertEquals("Test Project", project.projectName);
                                })
                                .replaceWithVoid();
        }

        @Test
        @RunOnVertxContext
        public Uni<Void> testGetProject() {
                CreateProjectRequest request = new CreateProjectRequest(
                                "tenant-1",
                                "Get Project Test",
                                "Description",
                                ProjectType.AGENTIC_AI,
                                "user-1",
                                new HashMap<>());

                return controlPlaneService.createProject(request)
                                .flatMap(created -> {
                                        assertNotNull(created.projectId);
                                        return controlPlaneService.getProject(created.projectId, "tenant-1")
                                                        .invoke(result -> {
                                                                assertNotNull(result);
                                                                assertEquals(created.projectId, result.projectId);
                                                        });
                                })
                                .replaceWithVoid();
        }
}
