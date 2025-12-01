
## 4. WAYANG-API MODULE

```
wayang-api/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── tech/kayys/wayang/api/
    │   │       ├── dto/
    │   │       │   ├── workflow/
    │   │       │   │   ├── CreateWorkflowRequest.java
    │   │       │   │   ├── UpdateWorkflowRequest.java
    │   │       │   │   ├── WorkflowResponse.java
    │   │       │   │   └── WorkflowListResponse.java
    │   │       │   │
    │   │       │   ├── execution/
    │   │       │   │   ├── ExecutionRequest.java
    │   │       │   │   ├── ExecutionResponse.java
    │   │       │   │   └── ExecutionStatusResponse.java
    │   │       │   │
    │   │       │   ├── node/
    │   │       │   │   ├── NodeConfigRequest.java
    │   │       │   │   └── NodeResponse.java
    │   │       │   │
    │   │       │   └── common/
    │   │       │       ├── PageRequest.java
    │   │       │       ├── PageResponse.java
    │   │       │       └── ApiError.java
    │   │       │
    │   │       ├── contract/
    │   │       │   ├── WorkflowService.java
    │   │       │   ├── ExecutionService.java
    │   │       │   ├── NodeService.java
    │   │       │   └── PluginService.java
    │   │       │
    │   │       └── mapper/
    │   │           ├── WorkflowMapper.java
    │   │           ├── ExecutionMapper.java
    │   │           └── NodeMapper.java
    │   │
    │   └── resources/
    │       └── openapi/
    │           └── wayang-api-spec.yaml
    │
    └── test/
        └── java/
            └── tech/kayys/wayang/api/
                └── mapper/
                    └── WorkflowMapperTest.java




                    ### wayang-core/wayang-api Package Structure

```
wayang-api/src/main/java/io/wayang/api/
├── node/
│   ├── Node.java                    # Core node interface
│   ├── NodeDescriptor.java          # Node metadata
│   ├── NodeContext.java             # Execution context
│   ├── NodeResult.java              # Execution result
│   └── NodeException.java           # Base exception
├── workflow/
│   ├── Workflow.java                # Workflow definition
│   ├── WorkflowSchema.java          # Schema definition
│   └── WorkflowExecutor.java        # Execution interface
├── spi/
│   ├── NodeProvider.java            # SPI for node implementations
│   └── ServiceProvider.java         # SPI for service implementations
└── model/
    ├── ExecutionContext.java        # Shared execution context
    └── Result.java                  # Generic result wrapper
```
