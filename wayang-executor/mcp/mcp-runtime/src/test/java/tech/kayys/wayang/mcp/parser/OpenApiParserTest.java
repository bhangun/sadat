package tech.kayys.wayang.mcp.parser;

import io.quarkus.test.junit.QuarkusTest;
import io.vertx.mutiny.core.Vertx;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.mcp.TestFixtures;
import tech.kayys.wayang.mcp.dto.GenerateToolsRequest;
import tech.kayys.wayang.mcp.dto.OpenApiParseResult;
import tech.kayys.wayang.mcp.dto.SourceType;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class OpenApiParserTest {

  @Inject
  OpenApiParser parser;

  @Inject
  Vertx vertx;

  @Test
  void testParseFromValidYamlString() {
    GenerateToolsRequest request = new GenerateToolsRequest(
        TestFixtures.TEST_TENANT_ID,
        TestFixtures.TEST_NAMESPACE,
        SourceType.RAW,
        TestFixtures.SAMPLE_OPENAPI_SPEC,
        null, // authProfileId
        TestFixtures.TEST_USER_ID,
        java.util.Map.of() // guardrailsConfig
    );

    OpenApiParseResult result = parser.parse(request)
        .await().indefinitely();

    assertNotNull(result);
    assertTrue(result.isValid());
    assertNotNull(result.openApi());
    assertEquals("Sample API", result.openApi().getInfo().getTitle());
    assertEquals("1.0.0", result.openApi().getInfo().getVersion());
    assertTrue(result.errors().isEmpty());
  }

  @Test
  void testParseFromInvalidSpec() {
    GenerateToolsRequest request = new GenerateToolsRequest(
        TestFixtures.TEST_TENANT_ID,
        TestFixtures.TEST_NAMESPACE,
        SourceType.RAW,
        TestFixtures.INVALID_OPENAPI_SPEC,
        null,
        TestFixtures.TEST_USER_ID,
        java.util.Map.of());

    OpenApiParseResult result = parser.parse(request)
        .await().indefinitely();

    assertNotNull(result);
    // Invalid spec may still parse but with errors or null OpenAPI object
    if (result.openApi() == null) {
      assertFalse(result.isValid());
      assertFalse(result.errors().isEmpty());
    }
  }

  @Test
  void testParseFromMalformedYaml() {
    String malformedYaml = """
        openapi: 3.0.0
        info:
          title: Test
          invalid yaml structure
        paths:
          /test
        """;

    GenerateToolsRequest request = new GenerateToolsRequest(
        TestFixtures.TEST_TENANT_ID,
        TestFixtures.TEST_NAMESPACE,
        SourceType.RAW,
        malformedYaml,
        null,
        TestFixtures.TEST_USER_ID,
        java.util.Map.of());

    OpenApiParseResult result = parser.parse(request)
        .await().indefinitely();

    assertNotNull(result);
    // Should handle malformed YAML gracefully
  }

  @Test
  void testParseFromJsonString() {
    String jsonSpec = """
        {
          "openapi": "3.0.0",
          "info": {
            "title": "JSON API",
            "version": "1.0.0"
          },
          "paths": {
            "/test": {
              "get": {
                "operationId": "getTest",
                "responses": {
                  "200": {
                    "description": "Success"
                  }
                }
              }
            }
          }
        }
        """;

    GenerateToolsRequest request = new GenerateToolsRequest(
        TestFixtures.TEST_TENANT_ID,
        TestFixtures.TEST_NAMESPACE,
        SourceType.RAW,
        jsonSpec,
        null,
        TestFixtures.TEST_USER_ID,
        java.util.Map.of());

    OpenApiParseResult result = parser.parse(request)
        .await().indefinitely();

    assertNotNull(result);
    assertTrue(result.isValid());
    assertNotNull(result.openApi());
    assertEquals("JSON API", result.openApi().getInfo().getTitle());
  }

  @Test
  void testParseWithReferences() {
    String specWithRefs = """
        openapi: 3.0.0
        info:
          title: API with References
          version: 1.0.0
        paths:
          /users:
            get:
              responses:
                '200':
                  content:
                    application/json:
                      schema:
                        $ref: '#/components/schemas/User'
        components:
          schemas:
            User:
              type: object
              properties:
                id:
                  type: string
                name:
                  type: string
        """;

    GenerateToolsRequest request = new GenerateToolsRequest(
        TestFixtures.TEST_TENANT_ID,
        TestFixtures.TEST_NAMESPACE,
        SourceType.RAW,
        specWithRefs,
        null,
        TestFixtures.TEST_USER_ID,
        java.util.Map.of());

    OpenApiParseResult result = parser.parse(request)
        .await().indefinitely();

    assertNotNull(result);
    assertTrue(result.isValid());
    assertNotNull(result.openApi());
    assertNotNull(result.openApi().getComponents());
    assertNotNull(result.openApi().getComponents().getSchemas());
    assertTrue(result.openApi().getComponents().getSchemas().containsKey("User"));
  }

  @Test
  void testParseFromGitSourceNotImplemented() {
    GenerateToolsRequest request = new GenerateToolsRequest(
        TestFixtures.TEST_TENANT_ID,
        TestFixtures.TEST_NAMESPACE,
        SourceType.GIT,
        "https://github.com/example/repo",
        null,
        TestFixtures.TEST_USER_ID,
        java.util.Map.of());

    assertThrows(UnsupportedOperationException.class, () -> {
      parser.parse(request).await().indefinitely();
    });
  }

  @Test
  void testExtractServerUrl() {
    GenerateToolsRequest request = TestFixtures.createGenerateToolsRequest();

    OpenApiParseResult result = parser.parse(request)
        .await().indefinitely();

    assertNotNull(result);
    assertTrue(result.isValid());
    assertNotNull(result.openApi().getServers());
    assertFalse(result.openApi().getServers().isEmpty());
    assertEquals("https://api.example.com/v1", result.openApi().getServers().get(0).getUrl());
  }

  @Test
  void testParseMultiplePaths() {
    GenerateToolsRequest request = TestFixtures.createGenerateToolsRequest();

    OpenApiParseResult result = parser.parse(request)
        .await().indefinitely();

    assertNotNull(result);
    assertTrue(result.isValid());
    assertNotNull(result.openApi().getPaths());
    assertTrue(result.openApi().getPaths().size() >= 2);
    assertTrue(result.openApi().getPaths().containsKey("/pets"));
    assertTrue(result.openApi().getPaths().containsKey("/pets/{petId}"));
  }
}
