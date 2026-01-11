package tech.kayys.wayang.mcp.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tech.kayys.wayang.mcp.TestFixtures;
import tech.kayys.wayang.mcp.dto.ToolGenerationResult;
import tech.kayys.wayang.mcp.dto.GenerateToolsRequest;
import tech.kayys.wayang.mcp.dto.SourceType;
import tech.kayys.wayang.mcp.parser.OpenApiToolGenerator;
import tech.kayys.wayang.mcp.service.McpToolExecutor;
import tech.kayys.wayang.mcp.runtime.ToolExecutionRequest;
import tech.kayys.wayang.mcp.runtime.ToolExecutionResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;

@QuarkusTest
class McpResourceTest {

        @InjectMock
        OpenApiToolGenerator toolGenerator;

        @InjectMock
        McpToolExecutor toolExecutor;

        @Test
        void testGenerateToolsSuccess() {
                // Mock successful tool generation
                GenerateToolsRequest request = new GenerateToolsRequest(
                                TestFixtures.TEST_TENANT_ID,
                                TestFixtures.TEST_NAMESPACE,
                                SourceType.RAW,
                                TestFixtures.SAMPLE_OPENAPI_SPEC,
                                null,
                                TestFixtures.TEST_USER_ID,
                                Map.of());

                ToolGenerationResult mockResult = new ToolGenerationResult(
                                UUID.randomUUID(),
                                request.namespace(),
                                3,
                                List.of("tool-1", "tool-2", "tool-3"),
                                List.of());

                Mockito.when(toolGenerator.generateTools(any(GenerateToolsRequest.class)))
                                .thenReturn(Uni.createFrom().item(mockResult));

                given()
                                .contentType(ContentType.JSON)
                                .body(request)
                                .when()
                                .post("/mcp/tools/generate")
                                .then()
                                .statusCode(200)
                                .body("namespace", equalTo(TestFixtures.TEST_NAMESPACE))
                                .body("toolCount", equalTo(3))
                                .body("toolIds", hasSize(3));
        }

        @Test
        void testGenerateToolsFailure() {
                // Mock tool generation failure
                Mockito.when(toolGenerator.generateTools(any(GenerateToolsRequest.class)))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Generation failed")));

                given()
                                .contentType(ContentType.JSON)
                                .body(Map.of(
                                                "tenantId", TestFixtures.TEST_TENANT_ID,
                                                "userId", TestFixtures.TEST_USER_ID,
                                                "namespace", TestFixtures.TEST_NAMESPACE,
                                                "sourceType", "RAW",
                                                "source", TestFixtures.INVALID_OPENAPI_SPEC))
                                .when()
                                .post("/mcp/tools/generate")
                                .then()
                                .statusCode(500)
                                .body("errors", hasSize(greaterThan(0)));
        }

        @Test
        void testExecuteToolSuccess() {
                // Mock successful tool execution
                ToolExecutionResult mockResult = ToolExecutionResult.success(
                                TestFixtures.TEST_TOOL_ID,
                                Map.of("status", "success", "data", Map.of("id", "123", "name", "Test")),
                                150L);

                Mockito.when(toolExecutor.execute(any(ToolExecutionRequest.class)))
                                .thenReturn(Uni.createFrom().item(mockResult));

                given()
                                .contentType(ContentType.JSON)
                                .body(Map.of(
                                                "tenantId", TestFixtures.TEST_TENANT_ID,
                                                "userId", TestFixtures.TEST_USER_ID,
                                                "toolId", TestFixtures.TEST_TOOL_ID,
                                                "arguments", Map.of("param1", "value1"),
                                                "workflowRunId", UUID.randomUUID().toString(),
                                                "agentId", "test-agent",
                                                "context", Map.of()))
                                .when()
                                .post("/mcp/tools/execute")
                                .then()
                                .statusCode(200)
                                .body("status", equalTo("SUCCESS"));
        }

        @Test
        void testExecuteToolFailure() {
                // Mock tool execution failure
                Mockito.when(toolExecutor.execute(any(ToolExecutionRequest.class)))
                                .thenReturn(Uni.createFrom().failure(new RuntimeException("Execution failed")));

                given()
                                .contentType(ContentType.JSON)
                                .body(Map.of(
                                                "tenantId", TestFixtures.TEST_TENANT_ID,
                                                "userId", TestFixtures.TEST_USER_ID,
                                                "toolId", TestFixtures.TEST_TOOL_ID,
                                                "arguments", Map.of("param1", "value1"),
                                                "workflowRunId", UUID.randomUUID().toString(),
                                                "agentId", "test-agent"))
                                .when()
                                .post("/mcp/tools/execute")
                                .then()
                                .statusCode(500)
                                .body("error", containsString("Execution failed"));
        }

        @Test
        void testListToolsReturnsEmpty() {
                // Currently returns empty list
                given()
                                .when()
                                .get("/mcp/tools")
                                .then()
                                .statusCode(200)
                                .body("$", hasSize(0));
        }

        @Test
        void testListToolsWithNamespaceFilter() {
                given()
                                .queryParam("namespace", TestFixtures.TEST_NAMESPACE)
                                .when()
                                .get("/mcp/tools")
                                .then()
                                .statusCode(200)
                                .body("$", hasSize(0));
        }

        @Test
        void testListToolsWithCapabilityFilter() {
                given()
                                .queryParam("capability", "READ_ONLY")
                                .when()
                                .get("/mcp/tools")
                                .then()
                                .statusCode(200)
                                .body("$", hasSize(0));
        }

        @Test
        void testGetToolReturns404() {
                // Currently returns 404
                given()
                                .when()
                                .get("/mcp/tools/" + TestFixtures.TEST_TOOL_ID)
                                .then()
                                .statusCode(404);
        }
}
