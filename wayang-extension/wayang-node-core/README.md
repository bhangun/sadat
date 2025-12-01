# Wayang Node Core

Core node execution and lifecycle management for the Wayang AI Agent Platform.

## Features

- **Dynamic Node Loading**: Support for multiple implementation types (Maven, WASM, Container)
- **Sandbox Isolation**: Three-level isolation strategy (TRUSTED, SEMI_TRUSTED, UNTRUSTED)
- **Resource Quotas**: Per-tenant and per-node resource management
- **Validation**: Schema-based input/output validation with JSON Schema support
- **Lifecycle Management**: Complete node lifecycle from initialization to cleanup
- **Observability**: Built-in metrics, tracing, and structured logging
- **Error Handling**: Comprehensive exception hierarchy with retry semantics
- **Audit Support**: Full provenance tracking for compliance

## Architecture

### Core Components

1. **Node Factory**: Creates node instances with appropriate isolation
2. **Node Loader**: Manages loading strategies based on sandbox level
3. **Isolation Manager**: Enforces security policies and capabilities
4. **Resource Quota Controller**: Tracks and enforces resource limits
5. **Validators**: Schema and capability validation
6. **Lifecycle Manager**: Handles node initialization and cleanup

### Isolation Levels

- **TRUSTED**: Minimal isolation, full capabilities
- **SEMI_TRUSTED**: Isolated classloader, limited capabilities
- **UNTRUSTED**: WASM or container isolation, restricted capabilities

## Usage

### Creating a Node

```java
@Inject
NodeFactoryRegistry factoryRegistry;

NodeDescriptor descriptor = loadDescriptor();
Node node = factoryRegistry.createNode(descriptor, true);
```

### Executing a Node

```java
NodeContext context = new NodeContext.Builder()
    .runId("run-123")
    .nodeId("node-456")
    .tenantId("tenant-789")
    .build();

context.setVariable("input1", "value");

ExecutionResult result = node.execute(context).toCompletableFuture().join();
```

### Validating Inputs

```java
@Inject
SchemaValidator validator;

ValidationResult result = validator.validateInputs(descriptor, context);
if (!result.valid()) {
    throw validator.toException(result);
}
```

## Configuration

See `application.properties` for all configuration options.

Key settings:
- `wayang.isolation.strict-mode`: Enable strict capability checking
- `wayang.quota.enabled`: Enable resource quota enforcement
- `wayang.node.cache.enabled`: Enable node instance caching

## Testing

Run tests:
```bash
mvn test
```

Run with coverage:
```bash
mvn verify
```

## Building

Build JAR:
```bash
mvn clean package
```

Build native image:
```bash
mvn clean package -Pnative
```

## License

Proprietary - Kayys Tech
```

This completes the comprehensive implementation of `wayang-node-core`! The module now includes:

✅ Complete domain models
✅ Factory and loader implementations
✅ Isolation and quota management
✅ Validation framework
✅ Lifecycle management
✅ Comprehensive exception hierarchy
✅ Full test coverage
✅ Production-ready configuration
✅ Documentation

The module is modular, thread-safe, observable, and follows all blueprint requirements for error handling, audit, and future-proofing.
