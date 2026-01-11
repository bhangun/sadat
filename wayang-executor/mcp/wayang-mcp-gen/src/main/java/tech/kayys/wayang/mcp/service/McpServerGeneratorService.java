package tech.kayys.wayang.mcp.service;

import io.quarkus.logging.Log;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Uni;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;

import tech.kayys.wayang.mcp.model.ApiOperation;
import tech.kayys.wayang.mcp.model.ApiParameter;
import tech.kayys.wayang.mcp.model.ApiSpecification;
import tech.kayys.wayang.mcp.model.McpParameterModel;
import tech.kayys.wayang.mcp.model.McpServerModel;
import tech.kayys.wayang.mcp.model.McpToolModel;
import tech.kayys.wayang.mcp.resource.McpGeneratorResource;
import tech.kayys.wayang.mcp.util.OpenApiConverter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@ApplicationScoped
public class McpServerGeneratorService {

    @Inject
    @io.quarkus.qute.Location("mcp-server-main.java")
    Template mcpServerTemplate;

    @Inject
    @io.quarkus.qute.Location("mcp-tool.java")
    Template mcpToolTemplate;

    @Inject
    @io.quarkus.qute.Location("mcp-websocket.java")
    Template mcpWebSocketTemplate;

    @Inject
    @io.quarkus.qute.Location("mcp-auth-handler.java")
    Template mcpAuthHandlerTemplate;

    @Inject
    @io.quarkus.qute.Location("generated-pom.xml")
    Template pomTemplate;

    @Inject
    @io.quarkus.qute.Location("generated-application.properties")
    Template applicationPropertiesTemplate;

    @Inject
    @io.quarkus.qute.Location("readme.md")
    Template readmeTemplate;

    @Inject
    @io.quarkus.qute.Location("dockerfile")
    Template dockerfileTemplate;

    @Inject
    @io.quarkus.qute.Location("docker-compose.yml")
    Template dockerComposeTemplate;

    @Inject
    PostmanParser postmanParser;

    @Inject
    SpecificationDetector specDetector;

    @Inject
    OpenApiValidator validator;

    private static final Pattern PACKAGE_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)*$");
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("^[A-Z][a-zA-Z0-9_]*$");

    public Uni<byte[]> generateMcpServer(InputStream inputFile, String filename, String packageName,
            String serverName, String baseUrl, boolean includeAuth,
            McpGeneratorResource.SpecificationType specType, String collectionName) {
        return Uni.createFrom().item(() -> {
            try {
                Log.info("Starting generation process for: " + filename + " (type: " + specType + ")");

                // Validate inputs
                validateInputs(packageName, serverName, filename);

                // Read file content
                String content = new String(inputFile.readAllBytes(), StandardCharsets.UTF_8);

                // Detect or use specified specification type
                McpGeneratorResource.SpecificationType detectedType = specType == McpGeneratorResource.SpecificationType.AUTO
                        ? specDetector.detectSpecificationType(content, filename)
                        : specType;

                Log.info("Processing as " + detectedType + " specification");

                // Parse specification based on type
                ApiSpecification apiSpec = parseSpecification(content, detectedType, collectionName);

                // Generate MCP server structure
                McpServerModel serverModel = createServerModel(apiSpec, packageName, serverName, baseUrl, includeAuth);
                Log.info("Generated server model with " + serverModel.getTools().size() + " tools");

                // Generate source code files
                Map<String, String> generatedFiles = generateSourceFiles(serverModel);
                Log.info("Generated " + generatedFiles.size() + " source files");

                // Create ZIP archive
                byte[] zipBytes = createZipArchive(generatedFiles);
                Log.info("Created ZIP archive: " + zipBytes.length + " bytes");

                return zipBytes;

            } catch (Exception e) {
                Log.error("Failed to generate MCP server", e);
                throw new RuntimeException("Failed to generate MCP server: " + e.getMessage(), e);
            }
        });
    }

    public Uni<ValidationResult> validateSpec(InputStream inputFile, String filename,
            McpGeneratorResource.SpecificationType specType) {
        return Uni.createFrom().item(() -> {
            try {
                String content = new String(inputFile.readAllBytes(), StandardCharsets.UTF_8);

                McpGeneratorResource.SpecificationType detectedType = specType == McpGeneratorResource.SpecificationType.AUTO
                        ? specDetector.detectSpecificationType(content, filename)
                        : specType;

                ApiSpecification apiSpec = parseSpecification(content, detectedType, "Validation");

                return new ValidationResult(
                        true,
                        List.of(),
                        List.of(),
                        extractApiInfo(apiSpec),
                        detectedType.name());

            } catch (Exception e) {
                return new ValidationResult(
                        false,
                        List.of("Failed to parse specification: " + e.getMessage()),
                        List.of(),
                        null,
                        "UNKNOWN");
            }
        });
    }

    public Uni<GenerationPreview> previewGeneration(InputStream inputFile, String filename,
            String packageName, String serverName,
            McpGeneratorResource.SpecificationType specType, String collectionName) {
        return Uni.createFrom().item(() -> {
            try {
                String content = new String(inputFile.readAllBytes(), StandardCharsets.UTF_8);

                McpGeneratorResource.SpecificationType detectedType = specType == McpGeneratorResource.SpecificationType.AUTO
                        ? specDetector.detectSpecificationType(content, filename)
                        : specType;

                ApiSpecification apiSpec = parseSpecification(content, detectedType, collectionName);
                McpServerModel serverModel = createServerModel(apiSpec, packageName, serverName,
                        "http://localhost:8080", false);

                return new GenerationPreview(
                        serverModel.getTitle(),
                        serverModel.getDescription(),
                        serverModel.getVersion(),
                        serverModel.getPackageName(),
                        serverModel.getServerName(),
                        detectedType.name(),
                        serverModel.getTools().stream()
                                .map(tool -> new ToolPreview(
                                        tool.getName(),
                                        tool.getDescription(),
                                        tool.getMethod(),
                                        tool.getPath(),
                                        tool.getParameters().size()))
                                .collect(Collectors.toList()));

            } catch (Exception e) {
                throw new RuntimeException("Failed to generate preview: " + e.getMessage(), e);
            }
        });
    }

    private ApiSpecification parseSpecification(String content, McpGeneratorResource.SpecificationType type,
            String collectionName) {
        return switch (type) {
            case OPENAPI -> parseOpenAPISpecification(content);
            case POSTMAN -> postmanParser.parsePostmanCollection(content, collectionName);
            case AUTO -> throw new IllegalStateException("AUTO type should be resolved before parsing");
        };
    }

    private ApiSpecification parseOpenAPISpecification(String content) {
        OpenAPI openAPI = parseOpenAPI(content);

        // Validate OpenAPI
        OpenApiValidator.ValidationResult validation = validator.validate(openAPI);
        if (!validation.isValid()) {
            throw new IllegalArgumentException("OpenAPI validation failed: " +
                    String.join(", ", validation.getErrors()));
        }

        // Log warnings if any
        if (!validation.getWarnings().isEmpty()) {
            Log.warn("OpenAPI validation warnings: " + String.join(", ", validation.getWarnings()));
        }

        return OpenApiConverter.convertToApiSpec(openAPI);
    }

    private void validateInputs(String packageName, String serverName, String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }

        if (!PACKAGE_NAME_PATTERN.matcher(packageName.toLowerCase()).matches()) {
            throw new IllegalArgumentException("Invalid package name format: " + packageName);
        }

        if (!CLASS_NAME_PATTERN.matcher(serverName).matches()) {
            throw new IllegalArgumentException("Invalid server name format: " + serverName);
        }
    }

    private OpenAPI parseOpenAPI(String content) {
        OpenAPIParser parser = new OpenAPIParser();
        SwaggerParseResult result = parser.readContents(content, null, null);

        if (result.getOpenAPI() == null) {
            throw new IllegalArgumentException("Invalid OpenAPI specification: " +
                    (result.getMessages() != null ? String.join(", ", result.getMessages()) : "Unknown error"));
        }

        if (result.getMessages() != null && !result.getMessages().isEmpty()) {
            Log.warn("OpenAPI parsing warnings: " + String.join(", ", result.getMessages()));
        }

        return result.getOpenAPI();
    }

    private McpServerModel createServerModel(ApiSpecification apiSpec, String packageName, String serverName,
            String baseUrl, boolean includeAuth) {
        McpServerModel model = new McpServerModel();
        model.setPackageName(packageName);
        model.setServerName(serverName);
        model.setServerClass(serverName);
        model.setBaseUrl(baseUrl);
        model.setIncludeAuth(includeAuth);

        // Set API info
        model.setTitle(apiSpec.getTitle() != null ? apiSpec.getTitle() : "Generated MCP Server");
        model.setDescription(apiSpec.getDescription() != null ? apiSpec.getDescription() : "");
        model.setVersion(apiSpec.getVersion() != null ? apiSpec.getVersion() : "1.0.0");

        // Override base URL if specified in spec
        if (apiSpec.getBaseUrl() != null) {
            model.setBaseUrl(apiSpec.getBaseUrl());
        }

        // Set security schemes if auth is included
        if (includeAuth && apiSpec.getSecuritySchemes() != null) {
            model.setSecuritySchemes(apiSpec.getSecuritySchemes());
        }

        // Generate tools from API operations
        List<McpToolModel> tools = apiSpec.getOperations().stream()
                .map(this::convertToMcpTool)
                .collect(Collectors.toList());

        model.setTools(tools);
        return model;
    }

    private McpToolModel convertToMcpTool(ApiOperation operation) {
        McpToolModel tool = new McpToolModel();

        // Generate tool name
        String toolName = operation.getOperationId() != null ? sanitizeToolName(operation.getOperationId())
                : generateToolNameFromPath(operation.getPath(), operation.getMethod());

        tool.setName(toolName);
        tool.setDescription(operation.getDescription() != null ? operation.getDescription()
                : operation.getSummary() != null ? operation.getSummary()
                        : "Execute " + operation.getMethod() + " " + operation.getPath());
        tool.setPath(operation.getPath());
        tool.setMethod(operation.getMethod());
        tool.setOperationId(operation.getOperationId());
        tool.setSummary(operation.getSummary());

        // Convert parameters
        List<McpParameterModel> parameters = (operation.getParameters() != null) ? operation.getParameters().stream()
                .map(this::convertToMcpParameter)
                .collect(Collectors.toList()) : new ArrayList<>();
        tool.setParameters(parameters);

        // Set response types
        tool.setResponseTypes(operation.getResponseTypes());

        // Set security requirements
        tool.setSecurityRequirements(operation.getSecurityRequirements());

        return tool;
    }

    private McpParameterModel convertToMcpParameter(ApiParameter apiParam) {
        McpParameterModel param = new McpParameterModel();
        param.setName(apiParam.getName());
        param.setDescription(apiParam.getDescription());
        param.setType(apiParam.getType());
        param.setRequired(apiParam.isRequired());
        param.setIn(apiParam.getIn());
        param.setExample(apiParam.getExample());
        param.setDefaultValue(apiParam.getDefaultValue());
        return param;
    }

    private String sanitizeToolName(String operationId) {
        // Convert to camelCase and ensure it's a valid identifier
        String sanitized = operationId.replaceAll("[^a-zA-Z0-9_]", "_");
        if (sanitized.matches("^[0-9].*")) {
            sanitized = "op_" + sanitized;
        }
        return toCamelCase(sanitized);
    }

    private String generateToolNameFromPath(String path, String method) {
        // Convert /api/users/{id} GET -> getApiUsersById
        String cleanPath = path
                .replaceAll("\\{([^}]+)\\}", "By$1") // {id} -> ById
                .replaceAll("[^a-zA-Z0-9_]", "_") // Replace special chars with _
                .replaceAll("_+", "_") // Collapse multiple underscores
                .replaceAll("^_|_$", ""); // Remove leading/trailing underscores

        String methodPrefix = method.toLowerCase();
        return methodPrefix + StringUtils.capitalize(toCamelCase(cleanPath));
    }

    private String toCamelCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String[] parts = input.split("_");
        StringBuilder result = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(StringUtils.capitalize(parts[i].toLowerCase()));
            }
        }
        return result.toString();
    }

    private ApiInfo extractApiInfo(ApiSpecification apiSpec) {
        return new ApiInfo(
                apiSpec.getTitle(),
                apiSpec.getDescription(),
                apiSpec.getVersion(),
                apiSpec.getOperations().size(),
                apiSpec.getOperations().size());
    }

    private Map<String, String> generateSourceFiles(McpServerModel model) {
        Map<String, String> files = new HashMap<>();
        String packagePath = model.getPackageName().replace(".", "/");

        try {
            // Generate main MCP server class
            TemplateInstance mainClass = mcpServerTemplate.data("model", model);
            files.put("src/main/java/" + packagePath + "/" + model.getServerClass() + ".java",
                    mainClass.render());

            // Generate WebSocket handler
            TemplateInstance wsHandler = mcpWebSocketTemplate.data("model", model);
            files.put("src/main/java/" + packagePath + "/websocket/McpWebSocketHandler.java",
                    wsHandler.render());

            // Generate auth handler if needed
            if (model.isIncludeAuth()) {
                TemplateInstance authHandler = mcpAuthHandlerTemplate.data("model", model);
                files.put("src/main/java/" + packagePath + "/auth/McpAuthHandler.java",
                        authHandler.render());
            }

            // Generate individual tool classes
            for (McpToolModel tool : model.getTools()) {
                TemplateInstance toolClass = mcpToolTemplate.data("model", model).data("tool", tool);
                String toolClassName = StringUtils.capitalize(tool.getName()) + "Tool";
                files.put("src/main/java/" + packagePath + "/tools/" + toolClassName + ".java",
                        toolClass.render());
            }

            // Generate configuration files
            files.put("pom.xml", pomTemplate.data("model", model).render());
            files.put("src/main/resources/application.properties",
                    applicationPropertiesTemplate.data("model", model).render());

            // Generate documentation
            files.put("README.md", readmeTemplate.data("model", model).render());

            // Generate Docker files
            files.put("Dockerfile", dockerfileTemplate.data("model", model).render());
            files.put("docker-compose.yml", dockerComposeTemplate.data("model", model).render());

            // Generate test files
            generateTestFiles(files, model, packagePath);

            // Generate additional utility files
            generateUtilityFiles(files, model, packagePath);

        } catch (Exception e) {
            Log.error("Error generating source files", e);
            throw new RuntimeException("Failed to generate source files: " + e.getMessage(), e);
        }

        return files;
    }

    private void generateUtilityFiles(Map<String, String> files, McpServerModel model, String packagePath) {
        // Generate .gitignore
        files.put(".gitignore", generateGitignore());

        // Generate GitHub Actions workflow
        files.put(".github/workflows/ci.yml", generateGitHubActions(model));

        // Generate VS Code launch configuration
        files.put(".vscode/launch.json", generateVSCodeLaunch());

        // Generate example scripts
        files.put("scripts/start-dev.sh", generateStartScript(true));
        files.put("scripts/start-prod.sh", generateStartScript(false));
        files.put("scripts/test-mcp.py", generatePythonTestScript(model));

        // Generate OpenAPI spec for documentation
        files.put("src/main/resources/meta-inf/openapi.yaml", "# Original OpenAPI spec would go here");
    }

    private String generateGitignore() {
        return """
                # Maven
                target/
                pom.xml.tag
                pom.xml.releaseBackup
                pom.xml.versionsBackup
                pom.xml.next
                release.properties
                dependency-reduced-pom.xml
                buildNumber.properties
                .mvn/timing.properties
                .mvn/wrapper/maven-wrapper.jar

                # IDE
                .idea/
                *.iml
                .vscode/
                .classpath
                .project
                .settings/
                *.swp
                *.swo

                # OS
                .DS_Store
                Thumbs.db

                # Logs
                *.log
                logs/

                # Application specific
                application-local.properties
                """;
    }

    private String generateGitHubActions(McpServerModel model) {
        return String.format("""
                name: CI/CD Pipeline

                on:
                  push:
                    branches: [ main, develop ]
                  pull_request:
                    branches: [ main ]

                jobs:
                  test:
                    runs-on: ubuntu-latest

                    steps:
                    - uses: actions/checkout@v3

                    - name: Set up JDK 17
                      uses: actions/setup-java@v3
                      with:
                        java-version: '17'
                        distribution: 'temurin'

                    - name: Cache Maven packages
                      uses: actions/cache@v3
                      with:
                        path: ~/.m2
                        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
                        restore-keys: ${{ runner.os }}-m2

                    - name: Run tests
                      run: ./mvnw clean test

                    - name: Build application
                      run: ./mvnw clean package -DskipTests

                  docker:
                    needs: test
                    runs-on: ubuntu-latest
                    if: github.ref == 'refs/heads/main'

                    steps:
                    - uses: actions/checkout@v3

                    - name: Set up JDK 17
                      uses: actions/setup-java@v3
                      with:
                        java-version: '17'
                        distribution: 'temurin'

                    - name: Build application
                      run: ./mvnw clean package -DskipTests

                    - name: Build Docker image
                      run: docker build -t %s:%s .

                    - name: Test Docker image
                      run: |
                        docker run -d -p 8080:8080 --name test-%s %s:%s
                        sleep 30
                        curl -f http://localhost:8080/health || exit 1
                        docker stop test-%s
                        docker rm test-%s
                """,
                model.getServerName().toLowerCase(),
                model.getVersion(),
                model.getServerName().toLowerCase(),
                model.getServerName().toLowerCase(),
                model.getVersion(),
                model.getServerName().toLowerCase(),
                model.getServerName().toLowerCase());
    }

    private String generateVSCodeLaunch() {
        return """
                {
                    "version": "0.2.0",
                    "configurations": [
                        {
                            "name": "Debug Quarkus App",
                            "type": "java",
                            "request": "attach",
                            "hostName": "localhost",
                            "port": 5005
                        },
                        {
                            "name": "Launch Quarkus Dev Mode",
                            "type": "java",
                            "request": "launch",
                            "mainClass": "io.quarkus.runner.GeneratedMain",
                            "args": [],
                            "preLaunchTask": "${workspaceFolder}/.vscode/tasks.json#quarkus-dev"
                        }
                    ]
                }
                """;
    }

    private String generateStartScript(boolean isDev) {
        if (isDev) {
            return """
                    #!/bin/bash
                    # Development start script

                    echo "Starting MCP Server in development mode..."
                    echo "Server will be available at http://localhost:8080"
                    echo "Press Ctrl+C to stop"

                    export QUARKUS_LOG_LEVEL=DEBUG
                    ./mvnw compile quarkus:dev
                    """;
        } else {
            return """
                    #!/bin/bash
                    # Production start script

                    echo "Starting MCP Server in production mode..."

                    # Build the application
                    echo "Building application..."
                    ./mvnw clean package -DskipTests

                    # Start the server
                    echo "Starting server..."
                    java -jar target/quarkus-app/quarkus-run.jar
                    """;
        }
    }

    private String generatePythonTestScript(McpServerModel model) {
        return String.format("""
                import requests
                import json
                import sys

                BASE_URL = "http://localhost:8080/mcp"

                def test_mcp_server():
                    print(f"Testing MCP Server: %s")

                    # Initialize
                    print("\\n1. Testing Initialize...")
                    payload = {
                        "jsonrpc": "2.0",
                        "id": 1,
                        "method": "initialize",
                        "params": {
                            "protocolVersion": "2024-11-05",
                            "capabilities": {},
                            "clientInfo": {"name": "python-test-client", "version": "1.0.0"}
                        }
                    }

                    try:
                        response = requests.post(BASE_URL, json=payload)
                        print(f"Status: {response.status_code}")
                        print(f"Response: {json.dumps(response.json(), indent=2)}")
                    except Exception as e:
                        print(f"Failed to connect: {e}")
                        return False

                    # List Tools
                    print("\\n2. Testing Tools List...")
                    payload = {
                        "jsonrpc": "2.0",
                        "id": 2,
                        "method": "tools/list"
                    }

                    response = requests.post(BASE_URL, json=payload)
                    print(f"Status: {response.status_code}")
                    tools = response.json().get("result", {}).get("tools", [])
                    print(f"Found {len(tools)} tools")

                    return True

                if __name__ == "__main__":
                    test_mcp_server()
                """, model.getServerName());
    }

    private void generateTestFiles(Map<String, String> files, McpServerModel model, String packagePath) {
        // Generate basic test class
        String testContent = generateTestClass(model);
        files.put("src/test/java/" + packagePath + "/" + model.getServerClass() + "Test.java", testContent);

        // Generate test resources
        files.put("src/test/resources/application-test.properties", generateTestProperties());
    }

    private String generateTestClass(McpServerModel model) {
        return String.format("""
                package %s;

                import io.quarkus.test.junit.QuarkusTest;
                import io.restassured.RestAssured;
                import org.junit.jupiter.api.Test;
                import org.junit.jupiter.api.BeforeEach;
                import org.junit.jupiter.api.DisplayName;

                import static io.restassured.RestAssured.given;
                import static org.hamcrest.Matchers.*;

                @QuarkusTest
                @DisplayName("%s Tests")
                public class %sTest {

                    @BeforeEach
                    void setup() {
                        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
                    }

                    @Test
                    @DisplayName("Health endpoint should return UP status")
                    public void testHealthEndpoint() {
                        given()
                            .when().get("/health")
                            .then()
                                .statusCode(200)
                                .body("status", equalTo("UP"))
                                .body("server", equalTo("%s"))
                                .body("version", equalTo("%s"));
                    }

                    @Test
                    @DisplayName("Server info endpoint should return correct information")
                    public void testServerInfo() {
                        given()
                            .when().get("/info")
                            .then()
                                .statusCode(200)
                                .body("name", equalTo("%s"))
                                .body("version", equalTo("%s"))
                                .body("protocol", equalTo("MCP"))
                                .body("protocolVersion", equalTo("2024-11-05"))
                                .body("availableTools", notNullValue())
                                .body("availableTools.size()", equalTo(%d));
                    }

                    @Test
                    @DisplayName("MCP initialize should return proper response")
                    public void testMcpInitialize() {
                        String initRequest = \"\"\"
                        {
                            "jsonrpc": "2.0",
                            "id": "1",
                            "method": "initialize",
                            "params": {
                                "protocolVersion": "2024-11-05",
                                "capabilities": {},
                                "clientInfo": {
                                    "name": "test-client",
                                    "version": "1.0.0"
                                }
                            }
                        }\"\"\";

                        given()
                            .header("Content-Type", "application/json")
                            .body(initRequest)
                            .when().post("/mcp")
                            .then()
                                .statusCode(200)
                                .body("jsonrpc", equalTo("2.0"))
                                .body("id", equalTo("1"))
                                .body("result.protocolVersion", equalTo("2024-11-05"))
                                .body("result.capabilities", notNullValue())
                                .body("result.serverInfo.name", equalTo("%s"))
                                .body("result.serverInfo.version", equalTo("%s"));
                    }

                    @Test
                    @DisplayName("Tools list should return all available tools")
                    public void testToolsList() {
                        String listRequest = \"\"\"
                        {
                            "jsonrpc": "2.0",
                            "id": "2",
                            "method": "tools/list"
                        }\"\"\";

                        given()
                            .header("Content-Type", "application/json")
                            .body(listRequest)
                            .when().post("/mcp")
                            .then()
                                .statusCode(200)
                                .body("jsonrpc", equalTo("2.0"))
                                .body("id", equalTo("2"))
                                .body("result.tools", notNullValue())
                                .body("result.tools.size()", equalTo(%d));
                    }

                    @Test
                    @DisplayName("Ping should return pong")
                    public void testPing() {
                        String pingRequest = \"\"\"
                        {
                            "jsonrpc": "2.0",
                            "id": "ping-test",
                            "method": "ping"
                        }\"\"\";

                        given()
                            .header("Content-Type", "application/json")
                            .body(pingRequest)
                            .when().post("/mcp")
                            .then()
                                .statusCode(200)
                                .body("jsonrpc", equalTo("2.0"))
                                .body("id", equalTo("ping-test"))
                                .body("result.status", equalTo("pong"))
                                .body("result.timestamp", notNullValue());
                    }

                    @Test
                    @DisplayName("Invalid JSON should return parse error")
                    public void testInvalidJson() {
                        given()
                            .header("Content-Type", "application/json")
                            .body("{ invalid json")
                            .when().post("/mcp")
                            .then()
                                .statusCode(400)
                                .body("jsonrpc", equalTo("2.0"))
                                .body("error.code", equalTo(-32700))
                                .body("error.message", containsString("Invalid JSON"));
                    }

                    @Test
                    @DisplayName("Unknown method should return method not found error")
                    public void testUnknownMethod() {
                        String unknownMethodRequest = \"\"\"
                        {
                            "jsonrpc": "2.0",
                            "id": "unknown-test",
                            "method": "unknown/method"
                        }\"\"\";

                        given()
                            .header("Content-Type", "application/json")
                            .body(unknownMethodRequest)
                            .when().post("/mcp")
                            .then()
                                .statusCode(200)
                                .body("jsonrpc", equalTo("2.0"))
                                .body("id", equalTo("unknown-test"))
                                .body("error.code", equalTo(-32601))
                                .body("error.message", containsString("Unknown method"));
                    }

                    @Test
                    @DisplayName("Tool call with non-existent tool should return error")
                    public void testNonExistentTool() {
                        String toolCallRequest = \"\"\"
                        {
                            "jsonrpc": "2.0",
                            "id": "tool-test",
                            "method": "tools/call",
                            "params": {
                                "name": "non_existent_tool",
                                "arguments": {}
                            }
                        }\"\"\";

                        given()
                            .header("Content-Type", "application/json")
                            .body(toolCallRequest)
                            .when().post("/mcp")
                            .then()
                                .statusCode(200)
                                .body("jsonrpc", equalTo("2.0"))
                                .body("id", equalTo("tool-test"))
                                .body("error.code", equalTo(-32602))
                                .body("error.message", containsString("Tool not found"));
                    }
                }
                """,
                model.getPackageName(),
                model.getTitle(),
                model.getServerClass(),
                model.getTitle(),
                model.getVersion(),
                model.getTitle(),
                model.getVersion(),
                model.getTools().size(),
                model.getTitle(),
                model.getVersion(),
                model.getTools().size());
    }

    private String generateTestProperties() {
        return """
                # Test Configuration
                quarkus.http.test-port=0
                quarkus.log.level=INFO
                quarkus.log.category."tech.kayys.wayang.mcp".level=DEBUG

                # Test API configuration
                api.base.url=http://localhost:${quarkus.http.test-port}

                # Test-specific settings
                mcp.server.name=Test MCP Server
                mcp.server.version=1.0.0-test

                # Disable external HTTP calls for testing
                api.mock.enabled=true
                """;
    }

    private byte[] createZipArchive(Map<String, String> files) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    // Response DTOs
    public static class ValidationResult {
        public final boolean valid;
        public final List<String> errors;
        public final List<String> warnings;
        public final ApiInfo apiInfo;
        public final String specType;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings, ApiInfo apiInfo,
                String specType) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
            this.apiInfo = apiInfo;
            this.specType = specType;
        }
    }

    public static class ApiInfo {
        public final String title;
        public final String description;
        public final String version;
        public final int pathCount;
        public final int operationCount;

        public ApiInfo(String title, String description, String version, int pathCount, int operationCount) {
            this.title = title;
            this.description = description;
            this.version = version;
            this.pathCount = pathCount;
            this.operationCount = operationCount;
        }
    }

    public static class GenerationPreview {
        public final String title;
        public final String description;
        public final String version;
        public final String packageName;
        public final String serverName;
        public final String specType;
        public final List<ToolPreview> tools;

        public GenerationPreview(String title, String description, String version,
                String packageName, String serverName, String specType, List<ToolPreview> tools) {
            this.title = title;
            this.description = description;
            this.version = version;
            this.packageName = packageName;
            this.serverName = serverName;
            this.specType = specType;
            this.tools = tools;
        }
    }

    public static class ToolPreview {
        public final String name;
        public final String description;
        public final String method;
        public final String path;
        public final int parameterCount;

        public ToolPreview(String name, String description, String method, String path, int parameterCount) {
            this.name = name;
            this.description = description;
            this.method = method;
            this.path = path;
            this.parameterCount = parameterCount;
        }
    }
}
