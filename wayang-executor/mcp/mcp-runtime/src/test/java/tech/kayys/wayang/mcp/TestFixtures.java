package tech.kayys.wayang.mcp;

import tech.kayys.wayang.mcp.domain.*;
import tech.kayys.wayang.mcp.dto.CreateAuthProfileRequest;
import tech.kayys.wayang.mcp.dto.SourceType;
import tech.kayys.wayang.mcp.dto.GenerateToolsRequest;
import tech.kayys.wayang.mcp.runtime.ToolExecutionRequest;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Test fixtures and sample data for tests
 */
public class TestFixtures {

  public static final String TEST_TENANT_ID = "test-tenant-001";
  public static final String TEST_USER_ID = "test-user-001";
  public static final String TEST_TOOL_ID = "test-tool-001";
  public static final String TEST_NAMESPACE = "test-namespace";

  /**
   * Sample OpenAPI 3.0 specification (Petstore)
   */
  public static final String SAMPLE_OPENAPI_SPEC = """
      openapi: 3.0.0
      info:
        title: Sample API
        version: 1.0.0
        description: A sample API for testing
      servers:
        - url: https://api.example.com/v1
      paths:
        /pets:
          get:
            operationId: listPets
            summary: List all pets
            parameters:
              - name: limit
                in: query
                schema:
                  type: integer
                  format: int32
            responses:
              '200':
                description: A list of pets
                content:
                  application/json:
                    schema:
                      type: array
                      items:
                        $ref: '#/components/schemas/Pet'
          post:
            operationId: createPet
            summary: Create a pet
            requestBody:
              required: true
              content:
                application/json:
                  schema:
                    $ref: '#/components/schemas/NewPet'
            responses:
              '201':
                description: Pet created
        /pets/{petId}:
          get:
            operationId: getPet
            summary: Get a pet by ID
            parameters:
              - name: petId
                in: path
                required: true
                schema:
                  type: string
            responses:
              '200':
                description: Pet details
                content:
                  application/json:
                    schema:
                      $ref: '#/components/schemas/Pet'
          delete:
            operationId: deletePet
            summary: Delete a pet
            parameters:
              - name: petId
                in: path
                required: true
                schema:
                  type: string
            responses:
              '204':
                description: Pet deleted
      components:
        schemas:
          Pet:
            type: object
            required:
              - id
              - name
            properties:
              id:
                type: string
              name:
                type: string
              tag:
                type: string
          NewPet:
            type: object
            required:
              - name
            properties:
              name:
                type: string
              tag:
                type: string
      """;

  /**
   * Invalid OpenAPI spec for testing error handling
   */
  public static final String INVALID_OPENAPI_SPEC = """
      openapi: 3.0.0
      info:
        title: Invalid API
      paths:
        /test:
          invalid_method:
            summary: This is invalid
      """;

  /**
   * Create a sample GenerateToolsRequest
   */
  public static GenerateToolsRequest createGenerateToolsRequest() {
    return new GenerateToolsRequest(
        TEST_TENANT_ID,
        TEST_NAMESPACE,
        SourceType.RAW,
        SAMPLE_OPENAPI_SPEC,
        null, // authProfileId
        TEST_USER_ID,
        Map.of() // guardrailsConfig
    );
  }

  /**
   * Create a sample ToolExecutionRequest
   */
  public static ToolExecutionRequest createToolExecutionRequest() {
    return new ToolExecutionRequest(
        TEST_TENANT_ID,
        TEST_USER_ID,
        TEST_TOOL_ID,
        Map.of("param1", (Object) "value1"),
        UUID.randomUUID().toString(), // workflowRunId
        "test-agent-001", // agentId
        Map.of() // context
    );
  }

  /**
   * Create a sample CreateAuthProfileRequest
   */
  public static CreateAuthProfileRequest createAuthProfileRequest() {
    return new CreateAuthProfileRequest(
        "Test Auth Profile",
        "BEARER",
        "Test authentication profile",
        "HEADER",
        "Authorization",
        "Bearer",
        "test-secret-value");
  }

  /**
   * Create a sample ToolGuardrails
   */
  public static ToolGuardrails createToolGuardrails() {
    ToolGuardrails guardrails = new ToolGuardrails();
    guardrails.setRateLimitPerMinute(10);
    guardrails.setRateLimitPerHour(100);
    guardrails.setRequireApproval(false);
    guardrails.setAllowedDomains(java.util.Set.of("api.example.com"));
    return guardrails;
  }

  /**
   * Create a sample ToolExecutionResult
   */
  public static tech.kayys.wayang.mcp.runtime.ToolExecutionResult createToolExecutionResult(boolean success) {
    if (success) {
      return tech.kayys.wayang.mcp.runtime.ToolExecutionResult.success(
          TEST_TOOL_ID,
          Map.of("result", "success"),
          150L);
    } else {
      return tech.kayys.wayang.mcp.runtime.ToolExecutionResult.failure(
          TEST_TOOL_ID,
          tech.kayys.wayang.mcp.model.InvocationStatus.FAILURE,
          "Test error message",
          150L);
    }
  }
}
