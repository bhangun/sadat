# {model.title}

{model.description}

## Overview

This is an automatically generated MCP (Model Context Protocol) server that can be generated from
multiple specification types:
- **OpenAPI/Swagger** (2.x, 3.x) specifications
- **Postman Collections** (v2.0, v2.1) exports

The server provides {model.tools.size()} tools that correspond to the API endpoints defined in the
original specification.

## Supported Input Formats

### OpenAPI/Swagger
- OpenAPI 3.0/3.1 (JSON/YAML)
- Swagger 2.0 (JSON/YAML)
- Automatic detection based on `openapi` or `swagger` fields

### Postman Collections
- Postman Collection v2.0/v2.1 (JSON)
- Exported collections from Postman app
- Collections with folders and nested requests
- Variable substitution support
- Authentication configurations

## Available Tools

{#for tool in model.tools}
### {tool.name}
- **Description**: {tool.description}
- **HTTP Method**: {tool.method}
- **Path**: {tool.path}
{#if tool.parameters}
- **Parameters**:
{#for param in tool.parameters}
- `{param.name}` ({param.javaType}){#if param.required} - **Required**{/if} [{param.in}]:
{param.description}
{/for}
{/if}

{/for}

## Generator Usage

The MCP server was generated using the following options:

### Command Line Example
```bash
curl -X POST "http://localhost:8080/api/mcp-generator/generate" \
-F "file=@your-spec.json" \
-F "specType=auto" \
-F "packageName={model.packageName}" \
-F "serverName={model.serverName}" \
-F "baseUrl={model.baseUrl}" \
-F "includeAuth={model.includeAuth?c}" \
-o generated-mcp-server.zip
```

### Supported Parameters
- `specType`: `auto` (default), `openapi`, `postman`
- `packageName`: Java package name for generated code
- `serverName`: Name of the generated MCP server class
- `baseUrl`: Base URL for API calls
- `includeAuth`: Enable authentication support (`true`/`false`)
- `collectionName`: Name for Postman collections (when specType=postman)

## Requirements

- Java 17 or later
- Maven 3.8.1 or later

## Configuration

Set the following environment variables or system properties:

- `API_BASE_URL`: Base URL for API calls (default: {model.baseUrl})
{#if model.includeAuth}
- `API_TOKEN`: Bearer token for authentication (if required)
- `API_KEY`: API key for authentication (if required)
- `API_USERNAME`: Username for basic auth (if required)
- `API_PASSWORD`: Password for basic auth (if required)
{/if}

### Postman Variable Support
If generated from a Postman collection, the server supports:
- Environment variables from the collection
- Collection-level variables
- Dynamic variable resolution at runtime
- Authentication configurations from Postman auth settings

## Building and Running

### Development Mode

```bash
./mvnw compile quarkus:dev
```

The server will start on `http://localhost:8080`

### Production Build

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

### Native Build

```bash
./mvnw package -Pnative
./target/{model.serverName?lower_case}-{model.version}-runner
```

### Docker

```bash
docker build -t {model.serverName?lower_case} .
docker run -p 8080:8080 {model.serverName?lower_case}
```

Or using Docker Compose:

```bash
docker-compose up
```

## Endpoints

- **MCP Protocol**: `POST /mcp` - Main MCP JSON-RPC endpoint
- **WebSocket**: `WS /mcp/ws` - WebSocket MCP endpoint
- **Health Check**: `GET /health` - Health status
- **Server Info**: `GET /info` - Server information and available tools

## MCP Protocol Usage

The server implements the Model Context Protocol (MCP) specification. It accepts JSON-RPC 2.0
requests.

### 1. Initialize Session

```json
{
"jsonrpc": "2.0",
"id": "1",
"method": "initialize",
"params": {
"protocolVersion": "2024-11-05",
"capabilities": {},
"clientInfo": {
"name": "example-client",
"version": "1.0.0"
}
}
}
```

### 2. List Available Tools

```json
{
"jsonrpc": "2.0",
"id": "2",
"method": "tools/list"
}
```

### 3. Call a Tool

```json
{
"jsonrpc": "2.0",
"id": "3",
"method": "tools/call",
"params": {
"name": "{model.tools.?first.name}",
"arguments": {
{#if model.tools.?first.parameters}
{#for param in model.tools.?first.parameters}
"{param.name}": "example_value"{#if param_hasNext},{/if}
{/for}
{/if}
}
}
}
```

## Testing

Run the test suite:

```bash
./mvnw test
```

### Test with Python Script

A Python test script is included for easy testing:

```bash
python3 scripts/test-mcp.py
```

The tests include:
- Health endpoint verification
- MCP protocol initialization
- Tools listing
- Basic tool execution
- Error handling scenarios

## Development

### Adding Custom Logic

You can extend the generated tools by modifying the classes in the `tools` package. Each tool class
implements the `McpTool` interface with an `execute` method.

### Postman-Specific Features

If generated from a Postman collection:
- Pre-request scripts are converted to parameter validation
- Test scripts are reflected in response handling
- Collection variables are supported through environment variables
- Folder structure is preserved in tool naming

### Authentication

{#if model.includeAuth}
The server supports multiple authentication methods:
- **Bearer Token**: Via `Authorization: Bearer
<token>` header
- **API Key**: Via `X-API-Key` header or query parameter
- **Basic Auth**: Via `Authorization: Basic
<credentials>` header

Authentication can be configured through:
- Environment variables (preferred for security)
- Tool call parameters (for dynamic auth)
- System properties

#### Postman Auth Integration
If generated from a Postman collection with authentication:
- Collection-level auth is automatically extracted
- Request-level auth overrides are supported
- OAuth2 flows are converted to bearer token auth
- API key locations (header/query) are preserved
{#else}
Authentication support was not included in this generation. To add authentication, regenerate with
the `includeAuth` option enabled.
{/if}

### Error Handling

The server provides comprehensive error handling with proper MCP error responses:
- Parameter validation errors
- HTTP client errors (timeouts, connection failures)
- API response errors (4xx, 5xx status codes)
- JSON parsing errors
- Authentication failures

### Monitoring

- Health checks are available at `/health`
- Detailed server info at `/info`
- WebSocket connections are logged
- All tool executions are logged with request/response details
- Metrics integration with Quarkus

## Specification-Specific Notes

### OpenAPI Features
- Full parameter type mapping
- Request/response schema validation
- Security scheme integration
- Server URL extraction
- Component references resolution

### Postman Features
- Variable interpolation (`{{variable}}` syntax)
- Environment and collection variables
- Folder organization preserved
- Request descriptions and examples
- Authentication configurations
- Pre/post-request script handling (limited)

## License

Generated code is provided as-is. Please review and modify according to your needs and security
requirements.

## Troubleshooting

### Common Issues

1. **Connection Refused**: Ensure the target API server is running and accessible
2. **Authentication Errors**: Verify API tokens/keys are correctly configured
3. **Timeout Errors**: Check network connectivity and API response times
4. **JSON Parse Errors**: Validate MCP request format against JSON-RPC 2.0 specification
5. **Variable Resolution Errors** (Postman): Check environment variable configuration

### Postman-Specific Issues

1. **Missing Variables**: Ensure all Postman variables are defined as environment variables
2. **Authentication Not Working**: Check if collection auth settings were properly converted
3. **Request Body Issues**: Verify JSON structure in raw body mode
4. **URL Building Errors**: Check path variable definitions and query parameters

### Debug Mode

Enable debug logging by setting:
```properties
quarkus.log.level=DEBUG
quarkus.log.category."{model.packageName}".level=DEBUG
```

Or via environment variable:
```bash
export QUARKUS_LOG_LEVEL=DEBUG
```

### Regeneration

If you encounter issues, try regenerating the server with different options:
- Explicitly set `specType=postman` or `specType=openapi`
- Enable authentication with `includeAuth=true`
- Adjust the base URL if API endpoints are not accessible
- Check the original specification for any validation errors/if}

### Error Handling

The server provides comprehensive error handling with proper MCP error responses. Check the logs for
detailed error information.

### Monitoring

- Health checks are available at `/health`
- Detailed server info at `/info`
- WebSocket connections are logged
- All tool executions are logged with request/response details

## License

Generated code is provided as-is. Please review and modify according to your needs and security
requirements.

## Troubleshooting

### Common Issues

1. **Connection Refused**: Ensure the target API server is running and accessible
2. **Authentication Errors**: Verify API tokens/keys are correctly configured
3. **Timeout Errors**: Check network connectivity and API response times
4. **JSON Parse Errors**: Validate MCP request format against JSON-RPC 2.0 specification

### Debug Mode

Enable debug logging by setting:
```properties
quarkus.log.level=DEBUG
```

Or via environment variable:
```bash
export QUARKUS_LOG_LEVEL=DEBUG
```