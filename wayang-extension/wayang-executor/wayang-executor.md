
## 8. WAYANG-EXECUTOR MODULE

```
wayang-executor/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/
    │   │   └── tech/kayys/wayang/executor/
    │   │       ├── api/
    │   │       │   └── ExecutorResource.java
    │   │       │
    │   │       ├── service/
    │   │       │   ├── NodeExecutor.java
    │   │       │   ├── SandboxedNodeExecutor.java
    │   │       │   └── ExecutorConfig.java
    │   │       │
    │   │       ├── sandbox/
    │   │       │   ├── ExecutionSandbox.java
    │   │       │   ├── JvmSandbox.java
    │   │       │   ├── WasmSandbox.java
    │   │       │   └── ContainerSandbox.java
    │   │       │
    │   │       ├── quota/
    │   │       │   ├── ResourceQuotaController.java
    │   │       │   ├── CpuQuotaEnforcer.java
    │   │       │   └── MemoryQuotaEnforcer.java
    │   │       │
    │   │       ├── adapter/
    │   │       │   ├── ToolAdapter.java
    │   │       │   ├── ModelAdapter.java
    │   │       │   ├── RAGAdapter.java
    │   │       │   └── MemoryAdapter.java
    │   │       │
    │   │       └── hook/
    │   │           ├── PreExecutionHook.java
    │   │           ├── PostExecutionHook.java
    │   │           └── HookChain.java
    │   │
    │   └── resources/
    │       └── application.properties
    │
    └── test/
        └── java/
            └── tech/kayys/wayang/executor/
                ├── sandbox/
                │   └── JvmSandboxTest.java
                └── service/
                    └── SandboxedNodeExecutorTest.java