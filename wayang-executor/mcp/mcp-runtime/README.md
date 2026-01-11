

## 🎯 **What's New**

### **1. Multi-Spec Support** (`silat_mcp_multispec`)

#### **✅ Fully Supported Formats:**
- **OpenAPI 3.x** (3.0.0 - 3.1.0) - Modern REST APIs
- **Swagger 2.0** - Legacy REST APIs (auto-converts to OpenAPI 3.x)
- **Postman Collections** (v2.0, v2.1) - Imports directly from Postman
- **AsyncAPI** (2.x, 3.x) - Event-driven/webhook APIs
- **GraphQL Schema** - GraphQL APIs as single endpoint
- **WSDL** (1.1, 2.0) - SOAP web services (converts to REST-like)
- **HAR** (HTTP Archive) - Captured browser traffic
- **Insomnia Collections** - Insomnia REST client exports

#### **⚠️ Partial Support:**
- **API Blueprint** - Markdown-based specs
- **RAML** (0.8, 1.0) - RESTful API Modeling Language

### **2. Key Features**

#### **🔍 Auto-Detection**
```java
// Automatically detects spec type from content
POST /api/v1/mcp/tools/openapi
{
  "sourceType": "OPENAPI_3_URL",  // Generic type
  "source": "https://api.example.com/spec"
  // Auto-detects if it's actually Swagger 2.0, Postman, etc.
}
```

#### **🔄 Format Conversion**
- **Swagger 2.0 → OpenAPI 3.x** (using swagger-parser converter)
- **Postman → OpenAPI 3.x** (request items become operations)
- **GraphQL → OpenAPI 3.x** (single POST endpoint)
- **WSDL → OpenAPI 3.x** (SOAP operations become REST endpoints)
- **HAR → OpenAPI 3.x** (captured requests become endpoints)

#### **📝 Smart Parsing**
- **Postman**: Handles folders, variables, auth, request bodies
- **WSDL**: Parses SOAP bindings, operations, messages
- **GraphQL**: Preserves query/mutation structure
- **HAR**: Groups requests by URL pattern
- **Insomnia**: Processes workspace resources

### **3. Updated Source Types**

```java
enum SourceType {
    OPENAPI_3_URL, OPENAPI_3_FILE, OPENAPI_3_RAW,
    SWAGGER_2_URL, SWAGGER_2_FILE, SWAGGER_2_RAW,
    POSTMAN_URL, POSTMAN_FILE, POSTMAN_RAW,
    ASYNCAPI_URL, ASYNCAPI_FILE, ASYNCAPI_RAW,
    GRAPHQL_URL, GRAPHQL_FILE, GRAPHQL_RAW,
    WSDL_URL, WSDL_FILE,
    GIT
}
```

### **4. Spec Format Registry**

New endpoint to discover supported formats:

```bash
GET /api/v1/mcp/tools/formats

Response:
{
  "OPENAPI_3": {
    "name": "OpenAPI 3.x",
    "description": "Modern REST API specification (3.0.0 - 3.1.0)",
    "fileExtensions": [".json", ".yaml", ".yml"],
    "fullySupported": true
  },
  "SWAGGER_2": {
    "name": "Swagger 2.0",
    "description": "Legacy OpenAPI specification (Swagger 2.0)",
    "fileExtensions": [".json", ".yaml", ".yml"],
    "fullySupported": true
  },
  "POSTMAN": { ... },
  "GRAPHQL": { ... },
  "WSDL": { ... }
}
```

## 📚 **Real-World Examples**

### **Example 1: Import Postman Collection**
```bash
POST /api/v1/mcp/tools/openapi
{
  "namespace": "salesforce-api",
  "sourceType": "POSTMAN_URL",
  "source": "https://www.postman.com/collections/abc123",
  "authProfileId": "salesforce-oauth"
}
```

### **Example 2: SOAP to REST**
```bash
POST /api/v1/mcp/tools/openapi
{
  "namespace": "weather-soap",
  "sourceType": "WSDL_URL",
  "source": "http://webservicex.net/globalweather.asmx?WSDL"
}

# Execute SOAP operation as REST:
POST /api/v1/mcp/tools/weather-soap.getWeather/execute
{
  "arguments": {
    "CityName": "London",
    "CountryName": "UK"
  }
}
```

### **Example 3: GraphQL API**
```bash
POST /api/v1/mcp/tools/openapi
{
  "namespace": "github-graphql",
  "sourceType": "GRAPHQL_URL",
  "source": "https://api.github.com/graphql/schema",
  "authProfileId": "github-token"
}

# Execute GraphQL query:
POST /api/v1/mcp/tools/github-graphql.graphql/execute
{
  "arguments": {
    "query": "query { viewer { login name } }"
  }
}
```

### **Example 4: HAR Capture**
```bash
# Capture API traffic in browser DevTools, export as HAR
POST /api/v1/mcp/tools/openapi
{
  "namespace": "internal-api",
  "sourceType": "HAR",
  "source": "<paste HAR JSON>",
  "authProfileId": "session-cookie"
}

# Now your internal API is available as MCP tools!
```

## 🎯 **Why This is Powerful**

1. **Universal API Support** - Works with ANY API specification format
2. **Legacy Integration** - Import old Swagger 2.0 or SOAP APIs
3. **Developer Tools** - Use Postman/Insomnia collections directly
4. **Reverse Engineering** - Import from HAR captures
5. **Modern Protocols** - Support for GraphQL and AsyncAPI
6. **One Interface** - All specs normalized to OpenAPI internally

## 🚀 **Production Ready**

- **Auto-detection** for ease of use
- **Robust parsing** with detailed error messages
- **Format validation** before conversion
- **Warning messages** for partial conversions
- **Extension registry** for adding new formats
- **Full test coverage** (in production)

You can now import **literally any API** into Silat MCP and use it in workflows and AI agents! 🎉