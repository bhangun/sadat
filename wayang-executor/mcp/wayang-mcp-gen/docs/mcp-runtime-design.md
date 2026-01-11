# Production-Grade MCP Server/Runtime Design

## Executive Summary

This document provides a **production-grade design** for an **MCP Server / Runtime** where **users only provide a Swagger / OpenAPI (OAS) URL, file, or raw spec string**, and the system automatically turns it into **runnable MCP tools** that can be safely used by agents at runtime.

**Key Features:**
- Uses documentation/descriptions from OpenAPI as tool capabilities
- Falls back to endpoint names when documentation is unavailable
- Allows users to enrich capabilities later
- Aligned with Wayang low-code agentic platform, Quarkus, and LangChain4j

---

## 1. Core Idea (What We're Building)

We are building an **OpenAPI → MCP Tool Engine**

### User Input (Only One Of):
1. **OpenAPI URL** - `https://api.example.com/openapi.json`
2. **Uploaded File** - `swagger.yaml` or `openapi.json`
3. **Raw Spec String** - JSON/YAML text

### System Output:
- **Runnable MCP Tools** that agents can invoke
- **Tool Metadata** with capabilities extracted from OpenAPI descriptions
- **Runtime Execution Engine** for safe API calls
- **Monitoring & Observability** for all tool invocations

---

## 2. High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     CONTROL PLANE                            │
│  (Wayang Platform - Tool Registration & Management)         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │   OpenAPI    │───▶│  Spec Parser │───▶│ Tool Builder │ │
│  │   Ingestion  │    │  & Validator │    │  & Registry  │ │
│  └──────────────┘    └──────────────┘    └──────────────┘ │
│         │                    │                    │         │
│         └────────────────────┴────────────────────┘         │
│                              │                               │
└──────────────────────────────┼───────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    EXECUTION PLANE                           │
│         (MCP Runtime - Tool Execution & Safety)              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │ MCP Protocol │───▶│  Tool Router │───▶│  API Client  │ │
│  │   Handler    │    │  & Executor  │    │   (HTTP)     │ │
│  └──────────────┘    └──────────────┘    └──────────────┘ │
│         │                    │                    │         │
│         │            ┌───────┴────────┐           │         │
│         │            │                │           │         │
│         │      ┌─────▼─────┐    ┌────▼────┐      │         │
│         │      │  Security │    │ Circuit │      │         │
│         │      │  & Auth   │    │ Breaker │      │         │
│         │      └───────────┘    └─────────┘      │         │
│         │                                         │         │
│         └─────────────────────────────────────────┘         │
│                                                              │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
                    ┌──────────────────┐
                    │  External APIs   │
                    │  (Target Systems)│
                    └──────────────────┘
```

---

## 3. Component Design

### 3.1 Control Plane Components

#### A. OpenAPI Ingestion Service
**Responsibility:** Accept and normalize OpenAPI specifications

```java
@ApplicationScoped
public class OpenApiIngestionService {
    
    public Uni<ParsedSpec> ingestFromUrl(String url) {
        // Download and validate OpenAPI from URL
        // Support authentication if needed
    }
    
    public Uni<ParsedSpec> ingestFromFile(InputStream file, String filename) {
        // Parse uploaded file (JSON/YAML)
        // Detect format and version
    }
    
    public Uni<ParsedSpec> ingestFromString(String content) {
        // Parse raw spec string
        // Auto-detect JSON vs YAML
    }
}
```

**Features:**
- Multi-format support (JSON, YAML)
- Version detection (OpenAPI 2.0, 3.0, 3.1)
- URL fetching with retry logic
- Content validation

#### B. Spec Parser & Validator
**Responsibility:** Parse and validate OpenAPI specifications

```java
@ApplicationScoped
public class SpecParserService {
    
    public Uni<ValidatedSpec> parseAndValidate(ParsedSpec spec) {
        // Parse OpenAPI structure
        // Extract operations, parameters, schemas
        // Validate against OpenAPI schema
        // Generate warnings for missing descriptions
    }
    
    public List<ToolCapability> extractCapabilities(ValidatedSpec spec) {
        // Extract from operation descriptions (priority 1)
        // Fall back to operation summaries (priority 2)
        // Fall back to endpoint names (priority 3)
        // Mark enrichment opportunities
    }
}
```

**Capability Extraction Logic:**
```java
public ToolCapability extractCapability(Operation operation, String path, String method) {
    String capability;
    boolean needsEnrichment = false;
    
    // Priority 1: Use description if available
    if (operation.getDescription() != null && !operation.getDescription().isEmpty()) {
        capability = operation.getDescription();
    }
    // Priority 2: Use summary
    else if (operation.getSummary() != null && !operation.getSummary().isEmpty()) {
        capability = operation.getSummary();
        needsEnrichment = true;
    }
    // Priority 3: Generate from endpoint
    else {
        capability = generateFromEndpoint(path, method);
        needsEnrichment = true;
    }
    
    return new ToolCapability(capability, needsEnrichment);
}
```

#### C. Tool Builder & Registry
**Responsibility:** Build MCP tool definitions and register them

```java
@ApplicationScoped
public class McpToolBuilderService {
    
    @Inject
    ToolRegistry registry;
    
    public Uni<List<McpTool>> buildTools(ValidatedSpec spec) {
        List<McpTool> tools = new ArrayList<>();
        
        for (Operation op : spec.getOperations()) {
            McpTool tool = McpTool.builder()
                .name(generateToolName(op))
                .description(extractCapability(op))
                .inputSchema(buildInputSchema(op))
                .metadata(buildMetadata(op))
                .needsEnrichment(checkNeedsEnrichment(op))
                .build();
                
            tools.add(tool);
        }
        
        return Uni.createFrom().item(tools);
    }
    
    public Uni<Void> registerTools(String specId, List<McpTool> tools) {
        // Register tools in the registry
        // Associate with spec ID for lifecycle management
    }
}
```

**Tool Metadata Structure:**
```java
public class McpToolMetadata {
    private String specId;              // Link to source OpenAPI spec
    private String operationId;         // Original OpenAPI operation ID
    private String path;                // API endpoint path
    private String method;              // HTTP method
    private boolean needsEnrichment;    // Flag for user enrichment
    private Map<String, String> auth;   // Authentication requirements
    private RateLimitConfig rateLimit;  // Rate limiting config
    private RetryConfig retry;          // Retry configuration
}
```

### 3.2 Execution Plane Components

#### A. MCP Protocol Handler
**Responsibility:** Handle MCP protocol communication

```java
@ApplicationScoped
public class McpProtocolHandler {
    
    @Inject
    ToolExecutor executor;
    
    public Uni<McpResponse> handleRequest(McpRequest request) {
        return switch (request.getMethod()) {
            case "tools/list" -> listTools();
            case "tools/call" -> callTool(request.getParams());
            case "initialize" -> initialize(request.getParams());
            default -> Uni.createFrom().item(
                McpResponse.error(-32601, "Method not found")
            );
        };
    }
    
    private Uni<McpResponse> callTool(JsonNode params) {
        String toolName = params.get("name").asText();
        JsonNode arguments = params.get("arguments");
        
        return executor.execute(toolName, arguments)
            .map(result -> McpResponse.success(result))
            .onFailure().recoverWithItem(
                error -> McpResponse.error(-32000, error.getMessage())
            );
    }
}
```

#### B. Tool Router & Executor
**Responsibility:** Route tool calls and execute them safely

```java
@ApplicationScoped
public class ToolExecutor {
    
    @Inject
    ApiClientFactory clientFactory;
    
    @Inject
    SecurityValidator securityValidator;
    
    @Inject
    CircuitBreakerRegistry circuitBreakers;
    
    public Uni<JsonNode> execute(String toolName, JsonNode arguments) {
        // 1. Lookup tool definition
        McpTool tool = registry.getTool(toolName);
        
        // 2. Validate security
        securityValidator.validate(tool, arguments);
        
        // 3. Build API request
        ApiRequest apiRequest = buildApiRequest(tool, arguments);
        
        // 4. Execute with circuit breaker
        CircuitBreaker cb = circuitBreakers.get(tool.getSpecId());
        
        return cb.executeSupplier(() -> 
            clientFactory.getClient(tool.getSpecId())
                .execute(apiRequest)
        );
    }
    
    private ApiRequest buildApiRequest(McpTool tool, JsonNode arguments) {
        McpToolMetadata meta = tool.getMetadata();
        
        return ApiRequest.builder()
            .method(meta.getMethod())
            .path(resolvePath(meta.getPath(), arguments))
            .headers(buildHeaders(meta, arguments))
            .queryParams(extractQueryParams(arguments))
            .body(extractBody(arguments))
            .build();
    }
}
```

#### C. API Client (HTTP)
**Responsibility:** Execute HTTP calls to external APIs

```java
@ApplicationScoped
public class ApiClient {
    
    @Inject
    @RestClient
    DynamicRestClient restClient;
    
    @Inject
    MetricsCollector metrics;
    
    public Uni<JsonNode> execute(ApiRequest request) {
        long startTime = System.currentTimeMillis();
        
        return restClient.execute(request)
            .invoke(response -> {
                long duration = System.currentTimeMillis() - startTime;
                metrics.recordApiCall(request, response, duration);
            })
            .map(this::parseResponse);
    }
    
    private JsonNode parseResponse(Response response) {
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            return response.readEntity(JsonNode.class);
        } else {
            throw new ApiExecutionException(
                response.getStatus(),
                response.readEntity(String.class)
            );
        }
    }
}
```

#### D. Security & Authentication
**Responsibility:** Handle API authentication and security

```java
@ApplicationScoped
public class SecurityValidator {
    
    @Inject
    CredentialStore credentialStore;
    
    public void validate(McpTool tool, JsonNode arguments) {
        McpToolMetadata meta = tool.getMetadata();
        
        // Check if authentication is required
        if (meta.getAuth() != null) {
            String authType = meta.getAuth().get("type");
            
            switch (authType) {
                case "bearer" -> validateBearerToken(tool, arguments);
                case "apiKey" -> validateApiKey(tool, arguments);
                case "oauth2" -> validateOAuth2(tool, arguments);
                case "basic" -> validateBasicAuth(tool, arguments);
            }
        }
        
        // Validate input against schema
        validateInputSchema(tool, arguments);
    }
    
    private void validateBearerToken(McpTool tool, JsonNode arguments) {
        // Check if token is provided in arguments or credential store
        // Validate token format
        // Check token expiration if possible
    }
}
```

#### E. Circuit Breaker & Resilience
**Responsibility:** Protect against cascading failures

```java
@ApplicationScoped
public class CircuitBreakerRegistry {
    
    private Map<String, CircuitBreaker> breakers = new ConcurrentHashMap<>();
    
    public CircuitBreaker get(String specId) {
        return breakers.computeIfAbsent(specId, id -> 
            CircuitBreaker.of(id, CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowSize(10)
                .build())
        );
    }
    
    public CircuitBreakerStatus getStatus(String specId) {
        CircuitBreaker cb = breakers.get(specId);
        return cb != null ? new CircuitBreakerStatus(
            cb.getState(),
            cb.getMetrics()
        ) : null;
    }
}
```

---

## 4. Data Models

### 4.1 MCP Tool Definition

```java
public class McpTool {
    private String name;                    // Tool name (e.g., "getUserById")
    private String description;             // Capability description
    private JsonNode inputSchema;           // JSON Schema for parameters
    private McpToolMetadata metadata;       // Execution metadata
    private boolean needsEnrichment;        // User should review/enrich
    private Instant createdAt;
    private Instant updatedAt;
}
```

### 4.2 Tool Registry Entry

```java
@Entity
public class RegisteredTool {
    @Id
    private String id;
    
    private String specId;                  // Source OpenAPI spec
    private String toolName;
    private String originalDescription;     // From OpenAPI
    private String enrichedDescription;     // User-provided enrichment
    private JsonNode inputSchema;
    private JsonNode metadata;
    private ToolStatus status;              // ACTIVE, DISABLED, NEEDS_REVIEW
    
    @CreationTimestamp
    private Instant createdAt;
    
    @UpdateTimestamp
    private Instant updatedAt;
}
```

### 4.3 Execution Log

```java
@Entity
public class ToolExecutionLog {
    @Id
    private String id;
    
    private String toolName;
    private String specId;
    private JsonNode input;
    private JsonNode output;
    private ExecutionStatus status;         // SUCCESS, FAILURE, TIMEOUT
    private String errorMessage;
    private Long durationMs;
    private Instant executedAt;
    
    // For analytics
    private String userId;
    private String sessionId;
}
```

---

## 5. API Endpoints

### 5.1 Control Plane APIs

```java
@Path("/api/v1/specs")
@Produces(MediaType.APPLICATION_JSON)
public class SpecManagementResource {
    
    @POST
    @Path("/from-url")
    public Uni<SpecRegistrationResponse> registerFromUrl(
        @QueryParam("url") String url
    ) {
        // Ingest, parse, build tools, register
    }
    
    @POST
    @Path("/from-file")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Uni<SpecRegistrationResponse> registerFromFile(
        @MultipartForm FileUpload upload
    ) {
        // Ingest, parse, build tools, register
    }
    
    @POST
    @Path("/from-string")
    @Consumes(MediaType.TEXT_PLAIN)
    public Uni<SpecRegistrationResponse> registerFromString(
        String specContent
    ) {
        // Ingest, parse, build tools, register
    }
    
    @GET
    @Path("/{specId}/tools")
    public Uni<List<McpTool>> getTools(
        @PathParam("specId") String specId
    ) {
        // List all tools for a spec
    }
    
    @PUT
    @Path("/{specId}/tools/{toolName}/enrich")
    public Uni<McpTool> enrichTool(
        @PathParam("specId") String specId,
        @PathParam("toolName") String toolName,
        ToolEnrichmentRequest request
    ) {
        // Allow user to provide better description
    }
}
```

### 5.2 Execution Plane APIs

```java
@Path("/mcp")
@Produces(MediaType.APPLICATION_JSON)
public class McpRuntimeResource {
    
    @POST
    public Uni<McpResponse> handleMcpRequest(McpRequest request) {
        // Handle all MCP protocol methods
    }
    
    @GET
    @Path("/health")
    public HealthCheckResponse health() {
        // Health check for runtime
    }
    
    @GET
    @Path("/metrics")
    public Uni<RuntimeMetrics> getMetrics() {
        // Runtime metrics and statistics
    }
}
```

---

## 6. Capability Extraction Strategy

### 6.1 Extraction Priority

```java
public class CapabilityExtractor {
    
    public ToolCapability extract(Operation operation, String path, String method) {
        // Priority 1: Operation description (best)
        if (hasDescription(operation)) {
            return new ToolCapability(
                operation.getDescription(),
                CapabilitySource.DESCRIPTION,
                false // No enrichment needed
            );
        }
        
        // Priority 2: Operation summary (good)
        if (hasSummary(operation)) {
            return new ToolCapability(
                operation.getSummary(),
                CapabilitySource.SUMMARY,
                true // Suggest enrichment
            );
        }
        
        // Priority 3: Generated from endpoint (fallback)
        String generated = generateFromEndpoint(path, method);
        return new ToolCapability(
            generated,
            CapabilitySource.GENERATED,
            true // Definitely needs enrichment
        );
    }
    
    private String generateFromEndpoint(String path, String method) {
        // GET /users/{id} -> "Get user by ID"
        // POST /orders -> "Create order"
        // DELETE /products/{id} -> "Delete product by ID"
        
        String action = getActionFromMethod(method);
        String resource = extractResourceFromPath(path);
        boolean hasParam = path.contains("{");
        
        if (hasParam) {
            return String.format("%s %s by ID", action, resource);
        } else {
            return String.format("%s %s", action, resource);
        }
    }
}
```

### 6.2 Enrichment UI Flow

```
1. User uploads OpenAPI spec
2. System generates tools
3. System flags tools needing enrichment
4. User reviews flagged tools
5. User provides better descriptions
6. System updates tool registry
7. Tools ready for agent use
```

---

## 7. Security & Safety

### 7.1 Input Validation

```java
public class InputValidator {
    
    public void validate(McpTool tool, JsonNode input) {
        // Validate against JSON Schema
        JsonSchema schema = JsonSchemaFactory.getInstance()
            .getSchema(tool.getInputSchema());
        
        Set<ValidationMessage> errors = schema.validate(input);
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
        
        // Additional security checks
        checkForInjection(input);
        checkForSensitiveData(input);
    }
    
    private void checkForInjection(JsonNode input) {
        // Check for SQL injection patterns
        // Check for command injection
        // Check for path traversal
    }
}
```

### 7.2 Rate Limiting

```java
@ApplicationScoped
public class RateLimiter {
    
    private Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();
    
    public boolean allowRequest(String toolName, String userId) {
        RateLimitBucket bucket = buckets.computeIfAbsent(
            toolName + ":" + userId,
            key -> new RateLimitBucket(100, Duration.ofMinutes(1))
        );
        
        return bucket.tryConsume();
    }
}
```

### 7.3 Credential Management

```java
@ApplicationScoped
public class CredentialStore {
    
    @Inject
    VaultClient vault;
    
    public String getCredential(String specId, String credentialType) {
        // Retrieve from HashiCorp Vault or similar
        return vault.read("mcp/specs/" + specId + "/" + credentialType);
    }
    
    public void storeCredential(String specId, String credentialType, String value) {
        // Store securely in Vault
        vault.write("mcp/specs/" + specId + "/" + credentialType, value);
    }
}
```

---

## 8. Monitoring & Observability

### 8.1 Metrics

```java
@ApplicationScoped
public class MetricsCollector {
    
    @Inject
    MeterRegistry registry;
    
    public void recordApiCall(ApiRequest request, Response response, long durationMs) {
        registry.counter("mcp.api.calls.total",
            "tool", request.getToolName(),
            "status", String.valueOf(response.getStatus())
        ).increment();
        
        registry.timer("mcp.api.duration",
            "tool", request.getToolName()
        ).record(Duration.ofMillis(durationMs));
    }
    
    public void recordToolExecution(String toolName, ExecutionStatus status) {
        registry.counter("mcp.tool.executions",
            "tool", toolName,
            "status", status.name()
        ).increment();
    }
}
```

### 8.2 Logging

```java
@ApplicationScoped
public class ExecutionLogger {
    
    @Inject
    ToolExecutionLogRepository repository;
    
    public void logExecution(ToolExecutionContext context, ToolExecutionResult result) {
        ToolExecutionLog log = new ToolExecutionLog();
        log.setToolName(context.getToolName());
        log.setSpecId(context.getSpecId());
        log.setInput(context.getInput());
        log.setOutput(result.getOutput());
        log.setStatus(result.getStatus());
        log.setDurationMs(result.getDurationMs());
        log.setExecutedAt(Instant.now());
        
        repository.persist(log);
    }
}
```

---

## 9. Deployment Architecture

### 9.1 Kubernetes Deployment

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mcp-runtime
spec:
  replicas: 3
  selector:
    matchLabels:
      app: mcp-runtime
  template:
    metadata:
      labels:
        app: mcp-runtime
    spec:
      containers:
      - name: mcp-runtime
        image: wayang/mcp-runtime:latest
        ports:
        - containerPort: 8080
        env:
        - name: QUARKUS_PROFILE
          value: "production"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /q/health/live
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /q/health/ready
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
```

### 9.2 Service Mesh Integration

```yaml
apiVersion: v1
kind: Service
metadata:
  name: mcp-runtime
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: "nlb"
spec:
  type: LoadBalancer
  selector:
    app: mcp-runtime
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
```

---

## 10. Future Enhancements

### 10.1 GraphQL Support
- Parse GraphQL schemas
- Generate MCP tools from queries/mutations
- Handle GraphQL-specific features

### 10.2 gRPC Support
- Parse Protocol Buffer definitions
- Generate MCP tools from gRPC services
- Handle streaming RPCs

### 10.3 AI-Powered Enrichment
- Use LLM to suggest better tool descriptions
- Auto-generate examples from schemas
- Semantic similarity matching for tool discovery

### 10.4 Tool Composition
- Allow chaining multiple tools
- Create workflows from tool sequences
- Visual workflow builder

---

## 11. Implementation Roadmap

### Phase 1: Core Foundation (Weeks 1-4)
- [ ] OpenAPI ingestion (URL, file, string)
- [ ] Spec parser and validator
- [ ] Basic tool builder
- [ ] Simple tool registry

### Phase 2: Execution Engine (Weeks 5-8)
- [ ] MCP protocol handler
- [ ] Tool executor
- [ ] HTTP API client
- [ ] Basic security validation

### Phase 3: Safety & Resilience (Weeks 9-12)
- [ ] Circuit breakers
- [ ] Rate limiting
- [ ] Input validation
- [ ] Credential management

### Phase 4: Observability (Weeks 13-16)
- [ ] Metrics collection
- [ ] Execution logging
- [ ] Monitoring dashboards
- [ ] Alerting

### Phase 5: Enrichment & Polish (Weeks 17-20)
- [ ] Capability extraction refinement
- [ ] Enrichment UI
- [ ] Documentation
- [ ] Production deployment

---

## 12. Success Metrics

### Technical Metrics
- **Tool Generation Time**: < 5 seconds for typical OpenAPI spec
- **Execution Latency**: < 100ms overhead (excluding API call)
- **Availability**: 99.9% uptime
- **Error Rate**: < 0.1% of tool executions

### Business Metrics
- **Time to Production**: Reduce from days to minutes
- **Developer Productivity**: 10x faster API integration
- **Tool Reusability**: 80% of tools used by multiple agents
- **User Satisfaction**: > 4.5/5 rating

---

## Conclusion

This production-grade design provides a comprehensive blueprint for building an MCP Server/Runtime that transforms OpenAPI specifications into runnable MCP tools. The architecture emphasizes:

1. **Simplicity**: Users only provide OpenAPI specs
2. **Intelligence**: Smart capability extraction with fallbacks
3. **Safety**: Comprehensive security and resilience
4. **Observability**: Full monitoring and logging
5. **Extensibility**: Plugin architecture for future enhancements

The system is ready for implementation using Quarkus, LangChain4j, and the Wayang platform ecosystem.
