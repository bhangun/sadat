# Silat MCP Server - Complete Usage Guide

## 🎯 Overview

The Silat MCP (Model Context Protocol) Server automatically transforms **any** API specification into safe, executable MCP tools for agentic AI workflows.

### ✅ Supported Specification Formats

| Format | Version | Status | Use Case |
|--------|---------|--------|----------|
| **OpenAPI 3.x** | 3.0.0 - 3.1.0 | ✅ Full | Modern REST APIs |
| **Swagger 2.0** | 2.0 | ✅ Full | Legacy REST APIs |
| **Postman** | v2.0, v2.1 | ✅ Full | Postman collections |
| **AsyncAPI** | 2.x, 3.x | ✅ Full | Event-driven APIs |
| **GraphQL** | Any | ✅ Full | GraphQL APIs |
| **WSDL** | 1.1, 2.0 | ✅ Full | SOAP web services |
| **HAR** | 1.2 | ✅ Full | Captured HTTP traffic |
| **Insomnia** | v4 | ✅ Full | Insomnia collections |
| **API Blueprint** | Any | ⚠️ Partial | Markdown APIs |
| **RAML** | 0.8, 1.0 | ⚠️ Partial | RAML specs |

---

## 📚 Example 1: OpenAPI 3.x (Stripe Payments API)

### Step 1: Generate Tools from OpenAPI URL

```bash
POST /api/v1/mcp/tools/openapi
Content-Type: application/json
X-Tenant-ID: acme-corp
Authorization: Bearer <token>

{
  "namespace": "stripe-payments",
  "sourceType": "OPENAPI_3_URL",
  "source": "https://raw.githubusercontent.com/stripe/openapi/master/openapi/spec3.yaml",
  "authProfileId": "stripe-prod-key",
  "guardrailsConfig": {
    "rateLimitPerMinute": 100,
    "maxExecutionTimeMs": 30000
  }
}
```

**Response:**
```json
{
  "sourceId": "550e8400-e29b-41d4-a716-446655440000",
  "namespace": "stripe-payments",
  "toolsGenerated": 157,
  "toolIds": [
    "stripe-payments.createCharge",
    "stripe-payments.retrieveCharge",
    "stripe-payments.createCustomer",
    "stripe-payments.createRefund",
    ...
  ],
  "warnings": []
}
```

### Step 2: Discover Generated Tools

```bash
GET /api/v1/mcp/tools?namespace=stripe-payments&capability=payment

Response:
[
  {
    "toolId": "stripe-payments.createCharge",
    "name": "createCharge",
    "description": "Create a charge",
    "capabilities": ["create charge", "payment processing"],
    "capabilityLevel": "WRITE",
    "readOnly": false,
    "tags": ["payments", "charges"]
  },
  {
    "toolId": "stripe-payments.retrieveCharge",
    "name": "retrieveCharge",
    "description": "Retrieve charge details",
    "capabilities": ["retrieve charge", "get payment"],
    "capabilityLevel": "READ_ONLY",
    "readOnly": true,
    "tags": ["payments", "charges"]
  }
]
```

### Step 3: Execute Tool

```bash
POST /api/v1/mcp/tools/stripe-payments.createCharge/execute

{
  "arguments": {
    "amount": 10000,
    "currency": "usd",
    "source": "tok_visa",
    "description": "Order #12345"
  }
}

Response:
{
  "status": "success",
  "output": {
    "id": "ch_3ABCxyz123",
    "amount": 10000,
    "currency": "usd",
    "status": "succeeded",
    "paid": true
  },
  "error": null,
  "executionTimeMs": 1243
}
```

---

## 📚 Example 2: Swagger 2.0 (Legacy API)

```bash
POST /api/v1/mcp/tools/openapi

{
  "namespace": "legacy-crm",
  "sourceType": "SWAGGER_2_URL",
  "source": "https://api.example.com/swagger.json",
  "authProfileId": "crm-api-key"
}

Response:
{
  "sourceId": "660e8400-e29b-41d4-a716-446655440001",
  "namespace": "legacy-crm",
  "toolsGenerated": 42,
  "toolIds": [
    "legacy-crm.getContacts",
    "legacy-crm.createContact",
    "legacy-crm.updateContact",
    ...
  ],
  "warnings": ["Converted from Swagger 2.0 to OpenAPI 3.x"]
}
```

---

## 📚 Example 3: Postman Collection

### Generate from Postman Collection JSON

```bash
POST /api/v1/mcp/tools/openapi

{
  "namespace": "salesforce-api",
  "sourceType": "POSTMAN_FILE",
  "source": "/uploads/salesforce-collection.json",
  "authProfileId": "salesforce-oauth"
}
```

**Or from Postman Cloud:**

```bash
POST /api/v1/mcp/tools/openapi

{
  "namespace": "salesforce-api",
  "sourceType": "POSTMAN_URL",
  "source": "https://www.postman.com/collections/abc123",
  "authProfileId": "salesforce-oauth"
}
```

---

## 📚 Example 4: GraphQL Schema

### Generate from GraphQL Schema

```bash
POST /api/v1/mcp/tools/openapi

{
  "namespace": "github-graphql",
  "sourceType": "GRAPHQL_URL",
  "source": "https://api.github.com/graphql/schema",
  "authProfileId": "github-token"
}

Response:
{
  "sourceId": "770e8400-e29b-41d4-a716-446655440002",
  "namespace": "github-graphql",
  "toolsGenerated": 1,
  "toolIds": [
    "github-graphql.graphql"
  ],
  "warnings": []
}
```

### Execute GraphQL Query as Tool

```bash
POST /api/v1/mcp/tools/github-graphql.graphql/execute

{
  "arguments": {
    "query": "query { viewer { login name email } }",
    "variables": {}
  }
}

Response:
{
  "status": "success",
  "output": {
    "data": {
      "viewer": {
        "login": "octocat",
        "name": "The Octocat",
        "email": "octocat@github.com"
      }
    }
  },
  "error": null,
  "executionTimeMs": 432
}
```

---

## 📚 Example 5: WSDL (SOAP Web Service)

### Generate from SOAP WSDL

```bash
POST /api/v1/mcp/tools/openapi

{
  "namespace": "weather-soap",
  "sourceType": "WSDL_URL",
  "source": "http://www.webservicex.net/globalweather.asmx?WSDL",
  "authProfileId": null
}

Response:
{
  "sourceId": "880e8400-e29b-41d4-a716-446655440003",
  "namespace": "weather-soap",
  "toolsGenerated": 2,
  "toolIds": [
    "weather-soap.getCitiesByCountry",
    "weather-soap.getWeather"
  ],
  "warnings": []
}
```

### Execute SOAP Operation as REST

```bash
POST /api/v1/mcp/tools/weather-soap.getWeather/execute

{
  "arguments": {
    "CityName": "London",
    "CountryName": "United Kingdom"
  }
}

Response:
{
  "status": "success",
  "output": {
    "Location": "London, United Kingdom",
    "Temperature": "15°C",
    "RelativeHumidity": "78%",
    "Wind": "SW 12 mph"
  },
  "error": null,
  "executionTimeMs": 2145
}
```

---

## 📚 Example 6: AsyncAPI (Event-Driven)

### Generate from AsyncAPI Spec

```bash
POST /api/v1/mcp/tools/openapi

{
  "namespace": "order-events",
  "sourceType": "ASYNCAPI_URL",
  "source": "https://api.example.com/asyncapi.yaml",
  "authProfileId": "kafka-credentials"
}
```

---

## 📚 Example 7: HAR File (Captured Traffic)

### Generate from Browser Capture

```bash
POST /api/v1/mcp/tools/openapi

{
  "namespace": "internal-api",
  "sourceType": "HAR",
  "source": "<HAR JSON content>",
  "authProfileId": "session-cookie"
}

Response:
{
  "sourceId": "990e8400-e29b-41d4-a716-446655440004",
  "namespace": "internal-api",
  "toolsGenerated": 15,
  "toolIds": [
    "internal-api.get_users",
    "internal-api.post_login",
    "internal-api.get_dashboard"
  ],
  "warnings": ["Generated from HTTP traffic - may need manual refinement"]
}
```

---

## 📚 Example 8: Insomnia Collection

```bash
POST /api/v1/mcp/tools/openapi

{
  "namespace": "notion-api",
  "sourceType": "INSOMNIA",
  "source": "/uploads/notion-insomnia.json",
  "authProfileId": "notion-integration"
}
```

---

## 🔐 Authentication Profiles

### Create API Key Auth Profile

```bash
POST /api/v1/mcp/auth-profiles

{
  "profileName": "Stripe Production",
  "authType": "API_KEY",
  "location": "HEADER",
  "paramName": "Authorization",
  "scheme": "Bearer",
  "secretValue": "sk_live_abc123...",
  "description": "Stripe production API key"
}

Response:
{
  "profileId": "auth-profile-001",
  "profileName": "Stripe Production",
  "authType": "API_KEY",
  "enabled": true
}
```

### Create OAuth2 Auth Profile

```bash
POST /api/v1/mcp/auth-profiles

{
  "profileName": "Salesforce OAuth",
  "authType": "OAUTH2_CLIENT_CREDENTIALS",
  "location": "HEADER",
  "paramName": "Authorization",
  "scheme": "Bearer",
  "secretValue": "<client_secret>",
  "description": "Salesforce OAuth credentials"
}
```

---

## 🔄 Integration with Silat Workflows

### Use MCP Tool in Workflow

```java
// Define workflow with MCP tool executor
client.workflows()
    .create("payment-processing")
    .addNode(new NodeDefinitionDto(
        "charge-card",
        "Charge Customer Card",
        "TASK",
        "mcp-tool-executor",
        Map.of(
            "toolId", "stripe-payments.createCharge",
            "arguments", Map.of(
                "amount", "{{totalAmount}}",
                "currency", "usd",
                "customer", "{{customerId}}"
            )
        ),
        List.of("validate-order"),
        List.of(new TransitionDto("send-confirmation", null, "SUCCESS")),
        null, 60L, true
    ))
    .execute();
```

### Use in AI Agent

```java
@ApplicationScoped
public class PaymentAgent {
    
    @Inject
    McpToolExecutor mcpExecutor;
    
    @Inject
    LLMProvider llm;
    
    public Uni<String> processPaymentRequest(String userRequest) {
        // LLM decides which tool to use
        return llm.chat(userRequest, getAvailableTools())
            .flatMap(toolCall -> {
                // Execute selected MCP tool
                return mcpExecutor.execute(new ToolExecutionRequest(
                    toolCall.toolId(),
                    tenantId,
                    toolCall.arguments(),
                    Map.of(),
                    userId, null, agentId
                ));
            })
            .map(result -> formatResponse(result));
    }
    
    private List<ToolMetadata> getAvailableTools() {
        // Fetch available MCP tools for agent
        return List.of(
            new ToolMetadata(
                "stripe-payments.createCharge",
                "createCharge",
                "Create a payment charge",
                inputSchema,
                Set.of("payment", "charge"),
                CapabilityLevel.WRITE,
                false
            )
        );
    }
}
```

---

## 🛡️ Security & Guardrails

### Configure Tool-Specific Guardrails

```bash
PUT /api/v1/mcp/tools/stripe-payments.createCharge

{
  "enabled": true,
  "description": "Create payment charge (requires approval for amounts > $1000)",
  "tags": ["payments", "critical"],
  "guardrails": {
    "rateLimitPerMinute": 60,
    "maxExecutionTimeMs": 30000,
    "requiresApproval": true,
    "allowedDomains": ["api.stripe.com"],
    "maxInputSizeBytes": 10240,
    "redactPii": true
  }
}
```

---

## 📊 Monitoring & Analytics

### Query Tool Invocations

```bash
GET /api/v1/mcp/invocations?toolId=stripe-payments.createCharge&from=2025-01-01

Response:
{
  "invocations": [
    {
      "invocationId": "inv-001",
      "toolId": "stripe-payments.createCharge",
      "status": "SUCCESS",
      "executionTimeMs": 1243,
      "invokedAt": "2025-01-10T10:30:00Z",
      "userId": "user-123"
    }
  ],
  "totalInvocations": 1523,
  "successRate": 0.987,
  "avgExecutionTimeMs": 1150
}
```

---

## 🚀 Advanced Features

### Auto-Refresh from Source

```bash
POST /api/v1/mcp/sources/{sourceId}/sync

Response:
{
  "syncStatus": "COMPLETED",
  "toolsAdded": 3,
  "toolsUpdated": 12,
  "toolsRemoved": 1
}
```

### Bulk Tool Generation

```bash
POST /api/v1/mcp/tools/bulk

{
  "sources": [
    {
      "namespace": "stripe",
      "sourceType": "OPENAPI_3_URL",
      "source": "https://raw.githubusercontent.com/stripe/openapi/master/openapi/spec3.yaml"
    },
    {
      "namespace": "sendgrid",
      "sourceType": "OPENAPI_3_URL",
      "source": "https://api.sendgrid.com/v3/openapi.yaml"
    },
    {
      "namespace": "twilio",
      "sourceType": "OPENAPI_3_URL",
      "source": "https://raw.githubusercontent.com/twilio/twilio-oai/main/spec/json/twilio_api_v2010.json"
    }
  ],
  "defaultAuthProfileId": "default-api-key"
}

Response:
{
  "totalToolsGenerated": 847,
  "sources": [
    { "namespace": "stripe", "toolsGenerated": 157 },
    { "namespace": "sendgrid", "toolsGenerated": 89 },
    { "namespace": "twilio", "toolsGenerated": 601 }
  ]
}
```

---

## 💡 Best Practices

1. **Namespace Organization**: Use descriptive namespaces (e.g., `stripe-payments`, `salesforce-crm`)
2. **Auth Profile Management**: Create separate profiles for dev/staging/prod
3. **Guardrails**: Always configure rate limits and timeouts
4. **Capability Levels**: Review and adjust auto-detected capability levels
5. **Read-Only First**: Enable read-only tools first, then gradually enable write operations
6. **Monitoring**: Set up alerts for failed invocations and rate limit hits
7. **Versioning**: Include version in namespace for breaking API changes (e.g., `stripe-payments-v2`)

---

## 🔧 Troubleshooting

### Common Issues

**Issue**: Tool generation fails with "Invalid spec"
- **Solution**: Validate your OpenAPI/Swagger spec at https://editor.swagger.io

**Issue**: Authentication errors during execution
- **Solution**: Verify auth profile credentials in Vault

**Issue**: Rate limit exceeded
- **Solution**: Adjust `rateLimitPerMinute` in guardrails config

**Issue**: Timeout errors
- **Solution**: Increase `maxExecutionTimeMs` or optimize target API

---

## 📖 API Reference

Full API documentation available at:
- Swagger UI: `https://silat.example.com/swagger-ui`
- OpenAPI Spec: `https://silat.example.com/openapi.json`

---

## 🎯 Quick Start Checklist

- [ ] Create authentication profile
- [ ] Generate tools from your API spec
- [ ] Test tool execution with sample data
- [ ] Configure guardrails (rate limits, timeouts)
- [ ] Integrate with Silat workflow or AI agent
- [ ] Monitor invocations and performance
- [ ] Set up auto-refresh for spec updates

---

**That's it! You're now ready to transform any API into safe, executable MCP tools for your agentic AI workflows! 🚀**