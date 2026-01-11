package tech.kayys.wayang.resources;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.core.MediaType;
import tech.kayys.wayang.project.domain.IntegrationPattern;
import tech.kayys.wayang.project.dto.CreatePatternRequest;
import tech.kayys.wayang.project.dto.EIPPatternType;
import tech.kayys.wayang.project.dto.IntegrationExecutionResult;
import tech.kayys.wayang.project.service.ControlPlaneService;

@QuarkusTest
public class IntegrationResourceIT {

    @InjectMock
    ControlPlaneService controlPlaneService;

    @Test
    public void testCreatePattern() {
        UUID projectId = UUID.randomUUID();
        CreatePatternRequest request = new CreatePatternRequest(
                "Test Pattern", "Desc", EIPPatternType.MESSAGE_ROUTER, null, null, null, null);

        IntegrationPattern pattern = new IntegrationPattern();
        pattern.patternId = UUID.randomUUID();
        pattern.patternName = "Test Pattern";

        when(controlPlaneService.createIntegrationPattern(any(), any())).thenReturn(Uni.createFrom().item(pattern));

        given()
                .contentType(MediaType.APPLICATION_JSON)
                .queryParam("projectId", projectId)
                .body(request)
                .when()
                .post("/api/v1/control-plane/integrations")
                .then()
                .statusCode(201)
                .body("patternName", is("Test Pattern"));
    }

    @Test
    public void testExecutePattern() {
        UUID patternId = UUID.randomUUID();
        IntegrationExecutionResult result = new IntegrationExecutionResult(true, "success", new HashMap<>(), List.of());

        when(controlPlaneService.executeIntegrationPattern(any(), any())).thenReturn(Uni.createFrom().item(result));

        given()
                .pathParam("patternId", patternId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new HashMap<>())
                .when()
                .post("/api/v1/control-plane/integrations/{patternId}/execute")
                .then()
                .statusCode(200)
                .body("success", is(true))
                .body("transformedPayload", is("success"));
    }
}
