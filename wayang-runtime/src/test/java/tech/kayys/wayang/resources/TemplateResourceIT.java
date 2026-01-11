package tech.kayys.wayang.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.project.domain.WorkflowTemplate;
import tech.kayys.wayang.project.dto.CreateTemplateRequest;
import tech.kayys.wayang.project.dto.PublishResponse;
import tech.kayys.wayang.project.dto.TemplateType;
import tech.kayys.wayang.project.service.ControlPlaneService;

@QuarkusTest
public class TemplateResourceIT {

    @InjectMock
    ControlPlaneService controlPlaneService;

    @Test
    public void testCreateTemplate() {
        UUID projectId = UUID.randomUUID();
        CreateTemplateRequest request = new CreateTemplateRequest(
                "Test Template", "Desc", "1.0.0", tech.kayys.wayang.project.dto.TemplateType.AI_AGENT_WORKFLOW, null,
                List.of());

        WorkflowTemplate template = new WorkflowTemplate();
        template.templateId = UUID.randomUUID();
        template.templateName = "Test Template";

        when(controlPlaneService.createWorkflowTemplate(any(), any())).thenReturn(Uni.createFrom().item(template));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .queryParam("projectId", projectId)
                .body(request)
                .when()
                .post("/api/v1/control-plane/templates")
                .then()
                .statusCode(201)
                .body("templateName", is("Test Template"));
    }

    @Test
    public void testPublishTemplate() {
        UUID templateId = UUID.randomUUID();
        when(controlPlaneService.publishWorkflowTemplate(any())).thenReturn(Uni.createFrom().item("workflow-123"));

        given()
                .pathParam("templateId", templateId)
                .when()
                .post("/api/v1/control-plane/templates/{templateId}/publish")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("workflowDefinitionId", is("workflow-123"));
    }
}
